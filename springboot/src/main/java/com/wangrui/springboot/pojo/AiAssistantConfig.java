package com.wangrui.springboot.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 表 ai_assistant_config：管理员在后台配置的 AI 助手（单行 id=1）。
 * 系统提示词 = 人设（persona） + 推荐规则（recommendationRules，追加在末尾）。
 * 模型 / 连接相关字段若为空，则运行时回退到 application.yml（环境变量）。
 */
public class AiAssistantConfig {

    /** 默认人设：管理员未配置时退回此值。 */
    public static final String DEFAULT_PERSONA =
            "你是轻小说推荐助手，既可以日常闲聊也可以根据我的书库推荐书籍。\n"
                    + "【闲聊场景】当用户打招呼、问你是谁、或聊与推荐无关的话题时，用中文自然友好地回复。\n";

    /** 默认推荐规则：约束模型按约定 JSON 输出推荐书单。 */
    public static final String DEFAULT_RECOMMENDATION_RULES =
            "【推荐场景】当用户需要推荐轻小说时，请先思考推荐理由，再只输出一个 JSON 对象（不要 Markdown、不要多余文字）：\n"
                    + "{\"reply\":\"一句话推荐说明\",\"books\":[{\"novelId\":1,\"reason\":\"推荐理由\"}]}\n"
                    + "规则：1) novelId 只能来自候选书单，禁止编造；2) books 最多 5 本，没有合适就返回空数组并在 reply 说明；"
                    + "3) 结合用户偏好，避开不感兴趣标签/作者；4) reply 用中文、口语自然、不超过 60 字。";

    /** 默认模型（与 application.yml 保持一致）。 */
    public static final String DEFAULT_MODEL = "mimo-v2.6-pro";

    /** 默认中转站地址（与 application.yml 保持一致，已含 /v1）。 */
    public static final String DEFAULT_BASE_URL = "https://api.ls1223.cn/v1";

    /** 默认采样温度。 */
    public static final double DEFAULT_TEMPERATURE = 0.6;

    private Integer id;

    /** 管理员自定义的 AI 人设。 */
    private String persona;

    /** 推荐规则，追加在系统提示词末尾。 */
    private String recommendationRules;

    /** 选用的大模型名称。 */
    private String model;

    /** OpenAI 兼容接口地址（含 /v1）。 */
    private String baseUrl;

    /** API Key。 */
    private String apiKey;

    /** 采样温度（0~2）。 */
    private Double temperature;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPersona() {
        return persona;
    }

    public void setPersona(String persona) {
        this.persona = persona;
    }

    public String getRecommendationRules() {
        return recommendationRules;
    }

    public void setRecommendationRules(String recommendationRules) {
        this.recommendationRules = recommendationRules;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 默认配置：管理员未配置时退回此值（用于后台首次展示）。
     */
    public static AiAssistantConfig defaultConfig() {
        AiAssistantConfig c = new AiAssistantConfig();
        c.setId(1);
        c.setPersona(DEFAULT_PERSONA);
        c.setRecommendationRules(DEFAULT_RECOMMENDATION_RULES);
        c.setModel(DEFAULT_MODEL);
        c.setBaseUrl(DEFAULT_BASE_URL);
        c.setApiKey("");
        c.setTemperature(DEFAULT_TEMPERATURE);
        return c;
    }
}
