package com.wangrui.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangrui.springboot.mapper.AiChatMessageMapper;
import com.wangrui.springboot.mapper.AiChatSessionMapper;
import com.wangrui.springboot.pojo.AiChatMessage;
import com.wangrui.springboot.pojo.AiChatSession;
import com.wangrui.springboot.pojo.vo.AiChatRequest;
import com.wangrui.springboot.pojo.vo.AiChatResult;
import com.wangrui.springboot.service.AiAssistantService;
import com.wangrui.springboot.util.JwtUtil;
import com.wangrui.springboot.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
/**
 * 人工智能助手控制器
 * 提供与人工智能助手相关的API接口
 */
@RequestMapping("/api/ai-assistant")
public class AiAssistantController {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int SESSION_TITLE_LENGTH = 12;

    @Value("${upload.ai-images-dir:}")
    private String aiImagesDir;

    @Value("${ai.image.base-url:}")
    private String aiImageBaseUrl;

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final AiAssistantService assistantService;
    private final TaskExecutor aiExecutor;

    public AiAssistantController(AiChatSessionMapper sessionMapper,
                                 AiChatMessageMapper messageMapper,
                                 AiAssistantService assistantService,
                                 @Qualifier("aiExecutor") TaskExecutor aiExecutor) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.assistantService = assistantService;
        this.aiExecutor = aiExecutor;
    }

    /**
     * 获取用户所有会话
     * @param request
     * @return
     */
    @GetMapping("/sessions")
    public Result<List<AiChatSession>> sessions(HttpServletRequest request) {
        Integer userId = userId(request);
        if (userId == null) {
            return Result.error(401, "登录信息无效");
        }
        return Result.success(sessionMapper.selectByUserId(userId));
    }

    /**
     * 获取指定会话的所有消息
     * @param sessionId
     * @param request
     * @return
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<AiChatMessage>> messages(@PathVariable Long sessionId, HttpServletRequest request) {
        Integer userId = userId(request);
        if (userId == null) {
            return Result.error(401, "登录信息无效");
        }
        if (sessionId == null || sessionId <= 0) {
            return Result.error(400, "会话ID无效");
        }
        if (!ownsSession(userId, sessionId)) {
            return Result.error(403, "无权访问该会话");
        }
        return Result.success(messageMapper.selectBySessionId(sessionId));
    }

    /**
     * 与人工智能助手进行聊天
     * @param body
     * @param request
     * @return
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody AiChatRequest body, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        Integer userId = userId(request);
        String content = body == null ? null : body.getContent();
        String imageUrl = body == null ? null : body.getImageUrl();
        if (userId == null) {
            return errorEmitter(emitter, "登录信息无效");
        }
        if ((content == null || content.isBlank()) && (imageUrl == null || imageUrl.isBlank())) {
            return errorEmitter(emitter, "消息内容或图片不能为空");
        }

        String safeContent = (content == null || content.isBlank())
                ? "请根据上传的图片推荐轻小说"
                : content.trim();

        Long sessionId;
        try {
            if (body.getSessionId() == null || body.getSessionId() <= 0) {
                AiChatSession session = new AiChatSession();
                session.setUserId(userId);
                session.setTitle(titleOf(safeContent));
                sessionMapper.insert(session);
                sessionId = session.getId();
            } else {
                sessionId = body.getSessionId();
                AiChatSession session = sessionMapper.selectById(sessionId);
                if (session == null || !Objects.equals(session.getUserId(), userId)) {
                    return errorEmitter(emitter, "会话不存在或无权访问");
                }
                sessionMapper.touch(sessionId);
            }
        } catch (Exception e) {
            log.warn("创建/获取 AI 会话失败", e);
            return errorEmitter(emitter, "会话创建失败，请重试");
        }

        List<AiChatMessage> history = messageMapper.selectBySessionId(sessionId);

        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRole("USER");
        userMessage.setContent(safeContent);
        userMessage.setImageUrl(imageUrl);
        messageMapper.insert(userMessage);

        final Long finalSessionId = sessionId;
        aiExecutor.execute(() -> handleReply(finalSessionId, userId, history, safeContent, imageUrl, emitter));
        return emitter;
    }

    /**
     * 重命名会话标题
     * @param sessionId 会话ID
     * @param body      请求体，包含新标题 title
     * @param request   HTTP 请求（用于解析当前登录用户）
     * @return 统一返回结果
     */
    @PutMapping("/sessions/{sessionId}/title")
    public Result<Void> renameSession(@PathVariable Long sessionId,
                                      @RequestBody Map<String, Object> body,
                                      HttpServletRequest request) {
        Integer userId = userId(request);
        if (userId == null) {
            return Result.error(401, "登录信息无效");
        }
        if (sessionId == null || sessionId <= 0) {
            return Result.error(400, "会话ID无效");
        }
        if (!ownsSession(userId, sessionId)) {
            return Result.error(403, "无权操作该会话");
        }
        // 获取并清理新标题
        String title = body.get("title") == null ? "" : String.valueOf(body.get("title")).trim();
        if (title.isEmpty()) {
            return Result.error(400, "标题不能为空");
        }
        if (title.length() > 60) {
            title = title.substring(0, 60);
        }
        // 保存新标题
        sessionMapper.updateTitle(sessionId, title);
        return Result.success();
    }

    /**
     * 会话置顶 / 取消置顶
     * @param sessionId 会话ID
     * @param body      请求体，包含置顶开关 pinned
     * @param request   HTTP 请求（用于解析当前登录用户）
     * @return 统一返回结果
     */
    @PutMapping("/sessions/{sessionId}/pin")
    public Result<Void> pinSession(@PathVariable Long sessionId,
                                   @RequestBody Map<String, Object> body,
                                   HttpServletRequest request) {
        Integer userId = userId(request);
        if (userId == null) {
            return Result.error(401, "登录信息无效");
        }
        if (sessionId == null || sessionId <= 0) {
            return Result.error(400, "会话ID无效");
        }
        if (!ownsSession(userId, sessionId)) {
            return Result.error(403, "无权操作该会话");
        }
        // 解析置顶开关（兼容 true / 1）
        String raw = body.get("pinned") == null ? "" : String.valueOf(body.get("pinned"));
        boolean pinned = "true".equalsIgnoreCase(raw) || "1".equals(raw);
        // 写入置顶状态
        sessionMapper.updatePinned(sessionId, pinned ? 1 : 0);
        return Result.success();
    }
    /**
     * 删除会话（先删该会话下的消息，再删会话）
     * @param sessionId
     * @param request
     * @return
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId,
                                      HttpServletRequest request) {
        Integer userId = userId(request);
        if (userId == null) {
            return Result.error(401, "登录信息无效");
        }
        if (sessionId == null || sessionId <= 0) {
            return Result.error(400, "会话ID无效");
        }
        if (!ownsSession(userId, sessionId)) {
            return Result.error(403, "无权操作该会话");
        }
        // 先删除该会话下的消息
        messageMapper.deleteBySessionId(sessionId);
        // 再删除会话本身
        sessionMapper.deleteById(sessionId);
        return Result.success();
    }

    /**
     * 上传 AI 图片
     * @param file
     * @param request
     * @return
     * @param request
     * @return
     */
    @PostMapping("/upload-image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file,
                                                   HttpServletRequest request) {
        Integer userId = userId(request);
        if (userId == null) {
            return Result.error(401, "登录信息无效");
        }
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return Result.error(400, "仅支持图片文件");
        }
        if (file.getSize() > 8 * 1024 * 1024) {
            return Result.error(400, "图片不能超过 8MB");
        }
        try {
            String dir = (aiImagesDir == null || aiImagesDir.isBlank())
                    ? System.getProperty("user.dir") + "/../uploads/ai-images"
                    : aiImagesDir;
            Path targetDir = Paths.get(dir);
            Files.createDirectories(targetDir);
            String ext = extensionOf(contentType, file.getOriginalFilename());
            String name = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = targetDir.resolve(name);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return Result.success(Map.of("url", absoluteUrl(request, "/ai-images/" + name)));
        } catch (Exception e) {
            log.warn("AI 图片上传失败", e);
            return Result.error(500, "图片上传失败");
        }
    }

    /**
     * 处理 AI 助手回复
     * @param sessionId
     * @param userId
     * @param history
     * @param content
     * @param imageUrl
     * @param emitter
     */

    /**
     * 逐字推送答案文本，形成打字机流式效果
     * @param emitter SSE 发射器
     * @param text    需要流式推送的文本
     * @throws Exception 推送异常
     */
    private void streamReply(SseEmitter emitter, String text) throws Exception {
        if (text == null || text.isEmpty()) {
            return;
        }
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            String token = new String(Character.toChars(codePoint));
            emit(emitter, Map.of("type", "reply", "text", token));
            offset += Character.charCount(codePoint);
            Thread.sleep(15L);
        }
    }

    private void handleReply(Long sessionId, Integer userId, List<AiChatMessage> history,
                             String content, String imageUrl, SseEmitter emitter) {
        try {
            emit(emitter, Map.of("type", "session", "sessionId", sessionId));

            AiChatResult result = assistantService.answer(userId, history, content, imageUrl,
                    delta -> {
                        try {
                            emit(emitter, Map.of("type", "think", "text", delta));
                        } catch (Exception e) {
                            log.warn("推送思考过程失败 sessionId={}", sessionId, e);
                        }
                    });

            AiChatMessage assistant = new AiChatMessage();
            assistant.setSessionId(sessionId);
            assistant.setRole("ASSISTANT");
            assistant.setContent(result.getText());
            assistant.setThinking(result.getThinking());
            assistant.setBooksJson(MAPPER.writeValueAsString(result.getBooks()));
            messageMapper.insert(assistant);

            // 答案逐字流式推送，形成打字机效果
            streamReply(emitter, result.getText());
            emit(emitter, Map.of("type", "books", "books", result.getBooks()));
            emit(emitter, Map.of("type", "done"));
            emitter.complete();
        } catch (Exception e) {
            log.warn("AI 荐书处理异常 sessionId={}", sessionId, e);
            try {
                emit(emitter, Map.of("type", "error", "message", "AI 助手暂时不可用，请稍后重试。"));
                emitter.complete();
            } catch (Exception ignored) {
                emitter.completeWithError(e);
            }
        }
    }

    /**
     * 发送 SSE 事件
     * @param emitter
     * @param payload
     * @throws Exception
     */
    private void emit(SseEmitter emitter, Map<String, Object> payload) throws Exception {
        emitter.send(SseEmitter.event().name("message").data(MAPPER.writeValueAsString(payload)));
    }

    /**
     * 发送错误 SSE 事件
     * @param emitter
     * @param message
     * @return
     */
    private SseEmitter errorEmitter(SseEmitter emitter, String message) {
        try {
            emit(emitter, Map.of("type", "error", "message", message));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /**
     * 检查用户是否拥有会话
     * @param userId
     * @param sessionId
     * @return
     */
    private boolean ownsSession(Integer userId, Long sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        return session != null && Objects.equals(session.getUserId(), userId);
    }

    /**
     * 从内容中提取会话标题
     * @param content
     * @return
     */
    private String titleOf(String content) {
        String c = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (c.isEmpty()) {
            return "图片对话";
        }
        return c.length() <= SESSION_TITLE_LENGTH ? c : c.substring(0, SESSION_TITLE_LENGTH);
    }

    /**
     * 从内容类型和原始文件名中提取文件扩展名
     * @param contentType
     * @param originalName
     * @return
     */
    private String extensionOf(String contentType, String originalName) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("png")) return ".png";
        if (ct.contains("webp")) return ".webp";
        if (ct.contains("gif")) return ".gif";
        if (ct.contains("jpeg") || ct.contains("jpg")) return ".jpg";
        String name = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : ".jpg";
    }

    /**
     * 构建绝对 URL
     * @param request
     * @param path
     * @return
     */
    private String absoluteUrl(HttpServletRequest request, String path) {
        String base = aiImageBaseUrl;
        if (base == null || base.isBlank()) {
            int port = request.getServerPort();
            return request.getScheme() + "://" + request.getServerName()
                    + (port == 80 || port == 443 ? "" : ":" + port) + path;
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    /**
     * 从请求中提取用户 ID
     * @param request
     * @return
     */
    private Integer userId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        String token = auth.substring(7).trim();
        return JwtUtil.validateToken(token) ? JwtUtil.getUserIdFromToken(token) : null;
    }
}