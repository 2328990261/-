package com.wangrui.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangrui.springboot.mapper.AiAssistantConfigMapper;
import com.wangrui.springboot.pojo.AiAssistantConfig;
import com.wangrui.springboot.pojo.vo.AdminAiTestRequest;
import com.wangrui.springboot.pojo.vo.AiChatResult;
import com.wangrui.springboot.service.AiAssistantService;
import com.wangrui.springboot.util.ApiKeyCipher;
import com.wangrui.springboot.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI 助手后台管理（管理员）。
 * GET  /api/admin/ai-assistant-config     — 获取配置（API Key 脱敏展示）
 * PUT  /api/admin/ai-assistant-config     — 保存配置（API Key 加密入库；留空则保持不变）
 * POST /api/admin/ai-assistant/test       — 对话测试（SSE，使用草稿配置、不落库）
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminAiAssistantController {

    private static final Logger log = LoggerFactory.getLogger(AdminAiAssistantController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private AiAssistantConfigMapper aiAssistantConfigMapper;

    @Autowired
    private AiAssistantService aiAssistantService;

    @Autowired
    private ApiKeyCipher apiKeyCipher;

    @Autowired
    @Qualifier("aiExecutor")
    private TaskExecutor aiExecutor;

    /**
     * 获取当前的 AI 助手配置。没有记录时返回默认配置；API Key 脱敏返回。
     */
    @GetMapping("/ai-assistant-config")
    public Result<AiAssistantConfig> get() {
        AiAssistantConfig row;
        try {
            row = aiAssistantConfigMapper.selectById(1);
        } catch (Exception e) {
            row = null;
        }
        if (row == null) {
            row = AiAssistantConfig.defaultConfig();
        } else {
            fillDefaults(row);
        }
        maskApiKey(row);
        return Result.success(row);
    }

    /**
     * 保存/更新 AI 助手配置。API Key 加密入库；为保持“留空不变”，空值不覆盖已有密钥。
     */
    @PutMapping("/ai-assistant-config")
    public Result<Void> save(@RequestBody AiAssistantConfig body) {
        if (body == null) {
            return Result.error("请求体不能为空");
        }
        String persona = body.getPersona() == null ? "" : body.getPersona().trim();
        if (persona.isEmpty()) {
            return Result.error("人设内容不能为空");
        }
        if (persona.length() > 5000) {
            return Result.error("人设内容不能超过 5000 字");
        }
        String rules = body.getRecommendationRules() == null ? "" : body.getRecommendationRules().trim();
        if (rules.isEmpty()) {
            rules = AiAssistantConfig.DEFAULT_RECOMMENDATION_RULES;
        }
        if (rules.length() > 10000) {
            return Result.error("推荐规则不能超过 10000 字");
        }
        Double temperature = body.getTemperature();
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            return Result.error("温度系数须在 0～2 之间");
        }

        AiAssistantConfig existing;
        try {
            existing = aiAssistantConfigMapper.selectById(1);
        } catch (Exception e) {
            existing = null;
        }

        body.setId(1);
        body.setPersona(persona);
        body.setRecommendationRules(rules);
        body.setModel(trimToNull(body.getModel()));
        body.setBaseUrl(trimToNull(body.getBaseUrl()));
        body.setTemperature(temperature);

        String apiKeyInput = trimToNull(body.getApiKey());
        if (apiKeyInput != null) {
            body.setApiKey(apiKeyCipher.encrypt(apiKeyInput));
        } else if (existing != null) {
            body.setApiKey(existing.getApiKey());
        } else {
            body.setApiKey(null);
        }

        try {
            if (existing == null) {
                aiAssistantConfigMapper.insert(body);
            } else {
                aiAssistantConfigMapper.update(body);
            }
        } catch (Exception e) {
            return Result.error("保存失败：请确认已执行 sql/ai_assistant_config.sql 建表脚本。详情：" + e.getMessage());
        }
        return Result.success(null);
    }

    /**
     * 对话测试：管理员用当前表单（草稿）配置发起一次不落库的流式对话。
     * API Key 草稿留空时，后台使用已保存（解密）或环境变量中的密钥。
     */
    @PostMapping(value = "/ai-assistant/test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter test(@RequestBody AdminAiTestRequest body) {
        SseEmitter emitter = new SseEmitter(120_000L);
        String content = body == null ? null : body.getContent();
        if (content == null || content.isBlank()) {
            return errorEmitter(emitter, "测试输入不能为空");
        }
        aiExecutor.execute(() -> handleTest(body, emitter));
        return emitter;
    }

    private void handleTest(AdminAiTestRequest body, SseEmitter emitter) {
        try {
            AiChatResult result = aiAssistantService.test(body, delta -> {
                try {
                    emit(emitter, Map.of("type", "think", "text", delta));
                } catch (Exception e) {
                    log.warn("推送思考过程失败", e);
                }
            });

            streamReply(emitter, result.getText());
            List<?> books = result.getBooks() == null ? List.of() : result.getBooks();
            emit(emitter, Map.of("type", "books", "books", books));
            emit(emitter, Map.of("type", "done"));
            emitter.complete();
        } catch (Exception e) {
            log.warn("AI 助手后台测试异常", e);
            errorEmitter(emitter, "AI 助手暂时不可用，请检查 Base URL、API Key 或模型配置。");
        }
    }

    /**
     * 逐字推送答案文本，形成打字机流式效果。
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

    private void emit(SseEmitter emitter, Map<String, Object> payload) throws Exception {
        emitter.send(SseEmitter.event().name("message").data(MAPPER.writeValueAsString(payload)));
    }

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
     * GET 时把空字段回填为默认展示值，避免后台页面出现空白。
     */
    private void fillDefaults(AiAssistantConfig row) {
        if (row.getRecommendationRules() == null || row.getRecommendationRules().isBlank()) {
            row.setRecommendationRules(AiAssistantConfig.DEFAULT_RECOMMENDATION_RULES);
        }
        if (row.getModel() == null || row.getModel().isBlank()) {
            row.setModel(AiAssistantConfig.DEFAULT_MODEL);
        }
        if (row.getBaseUrl() == null || row.getBaseUrl().isBlank()) {
            row.setBaseUrl(AiAssistantConfig.DEFAULT_BASE_URL);
        }
        if (row.getTemperature() == null) {
            row.setTemperature(AiAssistantConfig.DEFAULT_TEMPERATURE);
        }
    }

    /**
     * 将 API Key 脱敏后返回，避免明文密钥出现在接口响应里。
     */
    private void maskApiKey(AiAssistantConfig row) {
        String stored = row.getApiKey();
        if (stored == null || stored.isBlank()) {
            row.setApiKey("");
            return;
        }
        String plain = apiKeyCipher.decrypt(stored);
        row.setApiKey(plain == null ? "" : apiKeyCipher.mask(plain));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
