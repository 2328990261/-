package com.wangrui.springboot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangrui.springboot.mapper.AiAssistantConfigMapper;
import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.mapper.UserDislikeMapper;
import com.wangrui.springboot.mapper.UserPreferenceTagMapper;
import com.wangrui.springboot.pojo.AiAssistantConfig;
import com.wangrui.springboot.pojo.AiChatMessage;
import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.UserPreferenceTag;
import com.wangrui.springboot.pojo.vo.AdminAiTestRequest;
import com.wangrui.springboot.pojo.vo.AiBookItem;
import com.wangrui.springboot.pojo.vo.AiChatResult;
import com.wangrui.springboot.service.AiAssistantService;
import com.wangrui.springboot.service.AiStreamCallback;
import com.wangrui.springboot.util.ApiKeyCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionChunk;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 荐书服务实现。
 *
 * <h3>流式输出架构</h3>
 * 使用 OpenAiApi 底层 API 发起 {@code stream=true} 的 Chat Completion 请求，
 * 逐 token 接收响应，将推理过程（reasoning_content）通过 {@link AiStreamCallback}
 * 实时推送给 Controller 层，Controller 再通过 SSE 推送到前端，形成"思考框"流式效果。
 *
 * <h3>Prompt 设计</h3>
 * System Prompt 要求模型先输出思考过程（reasoning_content），再输出 JSON。
 * 部分模型（如 mimo-v2.6-pro、deepseek-v4）天然支持 reasoning_content 字段，
 * 思考文本和正文分别在两个独立字段中传输，无需用 &lt;thinking&gt; 标签包裹。
 *
 * <h3>候选人筛选</h3>
 * 根据用户偏好标签、不感兴趣标签/作者/作品，从书库中筛选出最多 30 本候选书籍，
 * 优先展示匹配偏好标签的书籍，并按阅读量降序排列。
 */
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int CANDIDATE_LIMIT = 30;
    private static final int HISTORY_LIMIT = 20;
    private static final int BOOK_RESULT_LIMIT = 5;

    /** 运行期配置快照：人设 + 推荐规则 + 模型连接参数。 */
    private static final class RuntimeConfig {
        final String persona;
        final String rules;
        final String model;
        final String baseUrl;
        final String apiKey;
        final double temperature;

        RuntimeConfig(String persona, String rules, String model, String baseUrl, String apiKey, double temperature) {
            this.persona = persona;
            this.rules = rules;
            this.model = model;
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.temperature = temperature;
        }
    }

    /**
     * 读取后台配置，缺失字段回退到 application.yml（环境变量）。
     */
    private RuntimeConfig loadRuntimeConfig() {
        AiAssistantConfig cfg = null;
        try {
            cfg = aiConfigMapper.selectById(1);
        } catch (Exception e) {
            log.warn("读取 AI 助手配置失败，使用默认配置", e);
        }
        String persona = firstNonBlank(cfg == null ? null : cfg.getPersona(), AiAssistantConfig.DEFAULT_PERSONA);
        String rules = firstNonBlank(cfg == null ? null : cfg.getRecommendationRules(), AiAssistantConfig.DEFAULT_RECOMMENDATION_RULES);
        String modelName = firstNonBlank(cfg == null ? null : cfg.getModel(), this.model);
        String baseUrlVal = firstNonBlank(cfg == null ? null : cfg.getBaseUrl(), this.baseUrl);
        String apiKeyVal = firstNonBlank(storedApiKey(cfg), this.apiKey);
        double temp = (cfg != null && cfg.getTemperature() != null) ? cfg.getTemperature() : this.temperature;
        return new RuntimeConfig(persona, rules, modelName, baseUrlVal, apiKeyVal, temp);
    }

    private AiAssistantConfig readConfig() {
        try {
            return aiConfigMapper.selectById(1);
        } catch (Exception e) {
            log.warn("读取 AI 助手配置失败，使用默认配置", e);
            return null;
        }
    }

    private String storedApiKey(AiAssistantConfig cfg) {
        if (cfg == null || isBlank(cfg.getApiKey())) {
            return null;
        }
        return apiKeyCipher.decrypt(cfg.getApiKey());
    }

    private String resolveTestApiKey(String draft) {
        if (!isBlank(draft)) {
            return draft.trim();
        }
        return firstNonBlank(storedApiKey(readConfig()), this.apiKey);
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.completions-path:/chat/completions}")
    private String completionsPath;

    @Value("${spring.ai.openai.chat.options.model:mimo-v2.6-pro}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.6}")
    private double temperature;
    private final NovelBookMainMapper novelMapper;
    private final UserPreferenceTagMapper preferenceTagMapper;
    private final UserDislikeMapper dislikeMapper;
    private final AiAssistantConfigMapper aiConfigMapper;
    private final ApiKeyCipher apiKeyCipher;

    public AiAssistantServiceImpl(NovelBookMainMapper novelMapper,
                                  UserPreferenceTagMapper preferenceTagMapper,
                                  UserDislikeMapper dislikeMapper,
                                  AiAssistantConfigMapper aiConfigMapper,
                                  ApiKeyCipher apiKeyCipher) {
        this.novelMapper = novelMapper;
        this.preferenceTagMapper = preferenceTagMapper;
        this.dislikeMapper = dislikeMapper;
        this.aiConfigMapper = aiConfigMapper;
        this.apiKeyCipher = apiKeyCipher;
    }

    /**
     * 核心推荐方法：构建候选书单 → 调用大模型流式生成推荐。
     * <p>
     * 人设来源：管理员在后台配置（ai_assistant_config 表），未配置时退默认人设。
     * 如果 API Key 或 Base URL 未配置，直接返回兜底推荐。
     * 如果模型调用异常，也回退到规则推荐。
     *
     * @param callback 流式回调，用于实时推送模型推理过程到前端
     */
    @Override
    public AiChatResult answer(Integer userId, List<AiChatMessage> history, String content, String imageUrl,
                               AiStreamCallback callback) {
        List<AiBookItem> candidates = buildCandidates(userId);
        AiChatResult fallback = fallback(candidates);

        RuntimeConfig cfg = loadRuntimeConfig();
        if (isBlank(cfg.apiKey) || isBlank(cfg.baseUrl)) {
            return notifiedFallback(fallback, callback);
        }

        try {
            String systemPrompt = cfg.persona + "\n\n" + cfg.rules;
            String userText = buildUserPrompt(userId, history, candidates, content);
            AiChatResult parsed = streamAnswer(systemPrompt, userText, imageUrl, candidates,
                    cfg.model, cfg.baseUrl, cfg.apiKey, cfg.temperature, callback);
            return parsed != null ? parsed : notifiedFallback(fallback, callback);
        } catch (Exception e) {
            log.warn("AI 荐书调用失败，回退规则推荐 userId={}", userId, e);
            return notifiedFallback(fallback, callback);
        }
    }

    /**
     * 后台「对话测试」：用管理员提交的草稿配置直接生成回复，不落库。
     */
    @Override
    public AiChatResult test(AdminAiTestRequest req, AiStreamCallback callback) {
        if (req == null) {
            req = new AdminAiTestRequest();
        }
        String content = req.getContent() == null ? "" : req.getContent().trim();
        boolean recommend = !isBlank(req.getMode()) && !"persona".equalsIgnoreCase(req.getMode().trim());

        String persona = firstNonBlank(req.getPersona(), AiAssistantConfig.DEFAULT_PERSONA);
        String rules = firstNonBlank(req.getRecommendationRules(), AiAssistantConfig.DEFAULT_RECOMMENDATION_RULES);
        String modelName = firstNonBlank(req.getModel(), this.model);
        String baseUrlVal = firstNonBlank(req.getBaseUrl(), this.baseUrl);
        String apiKeyVal = resolveTestApiKey(req.getApiKey());
        double temp = req.getTemperature() != null ? req.getTemperature() : this.temperature;

        String systemPrompt = recommend ? persona + "\n\n" + rules : persona;
        List<AiBookItem> candidates = recommend ? buildAdminCandidates() : Collections.emptyList();
        AiChatResult fallback = recommend ? fallback(candidates) : textFallback();

        if (isBlank(apiKeyVal) || isBlank(baseUrlVal)) {
            return notifiedFallback(fallback, callback);
        }

        try {
            String userText = recommend ? buildAdminUserPrompt(candidates, content) : content;
            AiChatResult parsed = streamAnswer(systemPrompt, userText, null, candidates,
                    modelName, baseUrlVal, apiKeyVal, temp, callback);
            return parsed != null ? parsed : notifiedFallback(fallback, callback);
        } catch (Exception e) {
            log.warn("AI 助手后台测试调用失败", e);
            return notifiedFallback(fallback, callback);
        }
    }

    /**
     * 回退推荐：已通过 callback 推送思考文本（流式效果），再返回兜底结果。
     */
    private AiChatResult notifiedFallback(AiChatResult fallback, AiStreamCallback callback) {
        if (callback != null) {
            String thinking = fallback.getThinking();
            if (thinking != null && !thinking.isBlank()) {
                callback.onThinking(thinking);
            }
        }
        return fallback;
    }

    private AiChatResult streamAnswer(String systemPrompt, String userText, String imageUrl,
                                      List<AiBookItem> candidates, String modelName, String baseUrlVal,
                                      String apiKeyVal, double temperature, AiStreamCallback callback) {
        // 构建请求消息列表（system + user，支持图片输入）
        List<ChatCompletionMessage> messages = new ArrayList<>();
        messages.add(new ChatCompletionMessage(systemPrompt, ChatCompletionMessage.Role.SYSTEM));
        messages.add(buildUserMessage(userText, imageUrl));

        // 开启流式请求：stream=true 让 API 逐 token 返回（含 reasoning_content 推理过程）
        ChatCompletionRequest request = new ChatCompletionRequest(messages, modelName, temperature, true);

        // thinking 收集推理过程文本，body 收集正文（JSON）
        StringBuilder thinking = new StringBuilder();
        StringBuilder body = new StringBuilder();

        // 订阅流式响应：每个 chunk 中分离推理内容与正文
        openAiApi(baseUrlVal, apiKeyVal).chatCompletionStream(request)
                .doOnNext(chunk -> consumeChunk(chunk, thinking, body, callback))
                .blockLast(); // 阻塞直到流结束（当前线程位于 aiExecutor 线程池中）

        // 解析正文 JSON，提取 reply 与 books
        AiChatResult parsed = parseResponse(body.toString(), candidates);
        if (parsed != null) {
            // 优先使用流式 API 推理内容字段（reasoning_content），
            // 解析结果中的 thinking（来自旧版 <thinking> 标签提取）作为兜底
            String reasoning = thinking.toString().trim();
            if (!reasoning.isEmpty()) {
                parsed.setThinking(reasoning);
            }
            // 注意：思考文本已在 consumeChunk 中通过 callback 实时推送，
            // 此处无需再次调用 callback.onThinking()
        }
        return parsed;
    }

    /**
     * 构建用户消息：纯文本或文本+图片（多模态）。
     * OpenAI 兼容的 vision API 使用 MediaContent 列表表示多模态输入。
     */
    private ChatCompletionMessage buildUserMessage(String userText, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return new ChatCompletionMessage(userText, ChatCompletionMessage.Role.USER);
        }
        List<ChatCompletionMessage.MediaContent> parts = new ArrayList<>();
        parts.add(new ChatCompletionMessage.MediaContent(userText));
        parts.add(new ChatCompletionMessage.MediaContent(
                new ChatCompletionMessage.MediaContent.ImageUrl(imageUrl)));
        return new ChatCompletionMessage(parts, ChatCompletionMessage.Role.USER);
    }

    /**
     * 消费流式响应的单个 chunk。
     * 将 delta 中的推理过程（reasoning_content）实时推送给前端，
     * 同时分别收集推理文本和正文文本到 StringBuilder。
     * <p>
     * 部分模型（如 deepseek-v4）会将思考过程放在 reasoning_content 字段中而不是正文里，
     * 这样就可以在前端展示"思考框"效果，而正文只包含最终的 JSON 回复。
     */
    private void consumeChunk(ChatCompletionChunk chunk, StringBuilder thinking, StringBuilder body,
                              AiStreamCallback callback) {
        if (chunk == null || chunk.choices() == null) {
            return;
        }
        for (ChatCompletionChunk.ChunkChoice choice : chunk.choices()) {
            ChatCompletionMessage delta = choice == null ? null : choice.delta();
            if (delta == null) {
                continue;
            }
            String reasoning = delta.reasoningContent();
            if (reasoning != null && !reasoning.isEmpty()) {
                thinking.append(reasoning);
                // 实时推送推理过程给前端，形成思考框流式效果
                if (callback != null) {
                    callback.onThinking(reasoning);
                }
            }
            String text = delta.content();
            if (text != null && !text.isEmpty()) {
                body.append(text);
            }
        }
    }

    /**
     * 每次调用构建新的 OpenAiApi 实例（配置可能被后台实时修改，避免缓存过期）。
     * 手动构建 OpenAiApi 而非依赖 Spring Boot 自动配置，
     * 因为我们需要直接调用底层流式 API 来获取 reasoning_content 增量。
     */
    private OpenAiApi openAiApi(String baseUrlVal, String apiKeyVal) {
        String trimmedBaseUrl = baseUrlVal == null ? "" : baseUrlVal.trim();
        while (trimmedBaseUrl.endsWith("/")) {
            trimmedBaseUrl = trimmedBaseUrl.substring(0, trimmedBaseUrl.length() - 1);
        }
        return OpenAiApi.builder()
                .baseUrl(trimmedBaseUrl)
                .apiKey(apiKeyVal)
                .completionsPath(completionsPath)
                .build();
    }

    /**
     * 构建候选书单：排除用户不感兴趣的书籍，按偏好匹配度 + 阅读量排序。
     */
    private List<AiBookItem> buildCandidates(Integer userId) {
        Set<String> positive = positiveTags(userId);

        Set<Integer> dislikedNovelIds = new HashSet<>(dislikeMapper.selectNovelIdsByUserId(userId));
        Set<String> dislikedTags = dislikeMapper.selectTagsByUserId(userId).stream()
                .filter(Objects::nonNull).map(String::trim).collect(Collectors.toSet());
        Set<String> dislikedAuthors = dislikeMapper.selectAuthorsByUserId(userId).stream()
                .filter(Objects::nonNull).map(a -> a.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        List<NovelBook> eligible = new ArrayList<>();
        for (NovelBook book : novelMapper.selectAllNovels()) {
            if (book == null || book.getId() == null) {
                continue;
            }
            if (dislikedNovelIds.contains(book.getId())) {
                continue;
            }
            Set<String> bookTags = splitTags(book.getLabel());
            if (intersects(bookTags, dislikedTags)) {
                continue;
            }
            if (book.getAuthor() != null
                    && dislikedAuthors.contains(book.getAuthor().trim().toLowerCase(Locale.ROOT))) {
                continue;
            }
            eligible.add(book);
        }

        eligible.sort(Comparator
                .comparing((NovelBook b) -> intersects(splitTags(b.getLabel()), positive) ? 0 : 1)
                .thenComparing((NovelBook b) -> b.getReadCount() == null ? 0 : b.getReadCount(),
                        Comparator.reverseOrder())
                .thenComparing((NovelBook b) -> b.getId() == null ? 0 : b.getId()));

        List<AiBookItem> items = new ArrayList<>();
        int limit = Math.min(CANDIDATE_LIMIT, eligible.size());
        for (int i = 0; i < limit; i++) {
            items.add(toItem(eligible.get(i)));
        }
        return items;
    }

    /**
     * 后台测试用候选书池：按阅读量取书库热门前若干本（无用户画像）。
     */
    private List<AiBookItem> buildAdminCandidates() {
        List<NovelBook> all = new ArrayList<>(novelMapper.selectAllNovels());
        all.sort(Comparator
                .comparing((NovelBook b) -> b.getReadCount() == null ? 0 : b.getReadCount(),
                        Comparator.reverseOrder())
                .thenComparing((NovelBook b) -> b.getId() == null ? 0 : b.getId()));

        List<AiBookItem> items = new ArrayList<>();
        int limit = Math.min(CANDIDATE_LIMIT, all.size());
        for (int i = 0; i < limit; i++) {
            items.add(toItem(all.get(i)));
        }
        return items;
    }

    /**
     * 后台测试用用户 prompt：仅候选书单 + 最新问题（无画像字段）。
     */
    private String buildAdminUserPrompt(List<AiBookItem> candidates, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("候选书单（ID|书名|作者|标签）：\n");
        for (AiBookItem item : candidates) {
            sb.append(item.getNovelId()).append('|')
                    .append(nullToEmpty(item.getTitle())).append('|')
                    .append(nullToEmpty(item.getAuthor())).append('|')
                    .append(nullToEmpty(item.getTags())).append('\n');
        }
        sb.append("\n用户最新问题：").append(nullToEmpty(content));
        return sb.toString();
    }

    /**
     * 纯人设测试模式的兜底回复（不涉及书单）。
     */
    private AiChatResult textFallback() {
        AiChatResult result = new AiChatResult();
        result.setThinking("");
        result.setText("AI 助手暂时不可用，请检查 Base URL、API Key 或模型配置。");
        return result;
    }

    /**
     * 构建发送给大模型的用户 prompt，包含偏好标签 + 不感兴趣信息 + 候选书单 + 对话历史。
     */
    private String buildUserPrompt(Integer userId, List<AiChatMessage> history,
                                   List<AiBookItem> candidates, String content) {
        Set<String> positive = positiveTags(userId);
        Set<String> dislikedTags = dislikeMapper.selectTagsByUserId(userId).stream()
                .filter(Objects::nonNull).map(String::trim).collect(Collectors.toSet());
        Set<String> dislikedAuthors = dislikeMapper.selectAuthorsByUserId(userId).stream()
                .filter(Objects::nonNull).map(String::trim).collect(Collectors.toSet());

        StringBuilder sb = new StringBuilder();
        sb.append("用户偏好标签：").append(joinOrNone(positive)).append('\n');
        sb.append("不感兴趣标签：").append(joinOrNone(dislikedTags)).append('\n');
        sb.append("不感兴趣作者：").append(joinOrNone(dislikedAuthors)).append("\n\n");
        sb.append("候选书单（ID|书名|作者|标签）：\n");
        for (AiBookItem item : candidates) {
            sb.append(item.getNovelId()).append('|')
                    .append(nullToEmpty(item.getTitle())).append('|')
                    .append(nullToEmpty(item.getAuthor())).append('|')
                    .append(nullToEmpty(item.getTags())).append('\n');
        }

        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - HISTORY_LIMIT);
            sb.append("\n最近对话：\n");
            for (int i = start; i < history.size(); i++) {
                AiChatMessage m = history.get(i);
                String role = "USER".equalsIgnoreCase(m.getRole()) ? "用户" : "AI";
                sb.append(role).append("：").append(nullToEmpty(m.getContent())).append('\n');
            }
        }

        sb.append("\n用户最新问题：").append(nullToEmpty(content));
        return sb.toString();
    }

    /**
     * 解析模型返回的 JSON 正文，提取 reply 推荐语和 books 书单。
     * <p>
     * 兼容两种格式：纯 JSON 或 &lt;thinking&gt;...&lt;/thinking&gt;JSON 的混合格式。
     *
     * @param raw        模型正文输出（不含 reasoning_content 部分）
     * @param candidates 候选书单，用于校验 novelId 合法性
     * @return 解析后的推荐结果，解析失败返回 null
     */
    private AiChatResult parseResponse(String raw, List<AiBookItem> candidates) {
        String thinking = extractThinking(raw);
        String json = extractJson(raw);
        if (json == null) {
            // 非推荐场景（闲聊 / 日常对话）：模型没输出 JSON，将原始文本作为纯文本回复
            if (raw == null || raw.isBlank()) {
                return null;
            }
            AiChatResult chatResult = new AiChatResult();
            chatResult.setThinking(thinking);
            chatResult.setText(raw.trim());
            return chatResult;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            String reply = root.path("reply").asText("");

            Map<Integer, AiBookItem> byId = candidates.stream()
                    .collect(Collectors.toMap(AiBookItem::getNovelId, Function.identity(), (x, y) -> x));

            List<AiBookItem> books = new ArrayList<>();
            JsonNode array = root.path("books");
            if (array.isArray()) {
                for (JsonNode node : array) {
                    int novelId = node.path("novelId").asInt(0);
                    String reason = node.path("reason").asText("");
                    AiBookItem src = byId.get(novelId);
                    if (src == null) {
                        continue;
                    }
                    AiBookItem item = new AiBookItem();
                    item.setNovelId(src.getNovelId());
                    item.setTitle(src.getTitle());
                    item.setAuthor(src.getAuthor());
                    item.setTags(src.getTags());
                    item.setCover(src.getCover());
                    item.setReason(reason);
                    books.add(item);
                }
            }

            if (reply.isBlank() && books.isEmpty()) {
                return null;
            }
            AiChatResult result = new AiChatResult();
            result.setThinking(thinking);
            result.setText(reply.isBlank() ? "根据你的偏好，为你推荐下面几本～" : reply);
            result.setBooks(books);
            return result;
        } catch (Exception e) {
            log.warn("AI 返回内容解析失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 兜底推荐：当模型不可用时，按候选书单顺序返回前 5 本书。
     */
    private AiChatResult fallback(List<AiBookItem> candidates) {
        AiChatResult result = new AiChatResult();
        result.setThinking("根据你的偏好标签和候选书单进行匹配，为你挑选合适的轻小说。");
        result.setText("AI 助手暂时不可用，先按你的偏好为你挑了几本书～");
        List<AiBookItem> books = new ArrayList<>();
        int n = Math.min(BOOK_RESULT_LIMIT, candidates.size());
        for (int i = 0; i < n; i++) {
            AiBookItem item = candidates.get(i);
            item.setReason("符合你的偏好");
            books.add(item);
        }
        result.setBooks(books);
        return result;
    }

    private Set<String> positiveTags(Integer userId) {
        return preferenceTagMapper.selectByUserId(userId).stream()
                .filter(Objects::nonNull)
                .map(UserPreferenceTag::getTagName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private static AiBookItem toItem(NovelBook book) {
        AiBookItem item = new AiBookItem();
        item.setNovelId(book.getId());
        item.setTitle(book.getBookMainName());
        item.setAuthor(book.getAuthor());
        item.setTags(String.join("、", splitTags(book.getLabel())));
        item.setCover(book.getCover());
        return item;
    }

    private static Set<String> splitTags(String label) {
        if (label == null || label.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(label.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static boolean intersects(Set<String> a, Set<String> b) {
        for (String x : b) {
            if (a.contains(x)) {
                return true;
            }
        }
        return false;
    }

    private static String joinOrNone(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return "无";
        }
        return String.join("、", set);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String extractThinking(String raw) {
        if (raw == null) {
            return "";
        }
        int start = raw.indexOf("<thinking>");
        if (start < 0) {
            return "";
        }
        int contentStart = start + "<thinking>".length();
        int end = raw.indexOf("</thinking>");
        if (end <= contentStart) {
            return raw.substring(contentStart).trim();
        }
        return raw.substring(contentStart, end).trim();
    }

    private static String extractJson(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        int endTag = trimmed.lastIndexOf("</thinking>");
        if (endTag >= 0) {
            trimmed = trimmed.substring(endTag + "</thinking>".length()).trim();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return trimmed.substring(start, end + 1);
    }
}
