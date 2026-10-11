package com.wangrui.springboot.pojo.vo;

/**
 * 后台「AI 助手配置」对话测试请求体。
 * 管理员使用当前表单（草稿）值直接测试，无需先保存。
 */
public class AdminAiTestRequest {

    /** 人设（草稿）。 */
    private String persona;

    /** 推荐规则（草稿）。 */
    private String recommendationRules;

    /** 模型名（草稿）。 */
    private String model;

    /** OpenAI 兼容接口地址（草稿）。 */
    private String baseUrl;

    /** API Key（草稿）。 */
    private String apiKey;

    /** 采样温度（草稿）。 */
    private Double temperature;

    /** 测试模式：persona=仅人设闲聊；recommend=完整推荐（带书单）。 */
    private String mode;

    /** 测试输入文本。 */
    private String content;

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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
