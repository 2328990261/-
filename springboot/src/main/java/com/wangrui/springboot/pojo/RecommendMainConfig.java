package com.wangrui.springboot.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表 recommend_main_config：主路径 GET /api/recommend/books 的全局超参（单行 id=1）。
 */
public class RecommendMainConfig {
    private Integer id;
    private BigDecimal popWeight;
    private BigDecimal cfBlendLambda;
    private Integer cfMaxSeeds;
    private Integer cfPureSlotCap;
    private Integer mmrPoolCap;
    private BigDecimal mmrLambda;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getPopWeight() {
        return popWeight;
    }

    public void setPopWeight(BigDecimal popWeight) {
        this.popWeight = popWeight;
    }

    public BigDecimal getCfBlendLambda() {
        return cfBlendLambda;
    }

    public void setCfBlendLambda(BigDecimal cfBlendLambda) {
        this.cfBlendLambda = cfBlendLambda;
    }

    public Integer getCfMaxSeeds() {
        return cfMaxSeeds;
    }

    public void setCfMaxSeeds(Integer cfMaxSeeds) {
        this.cfMaxSeeds = cfMaxSeeds;
    }

    public Integer getCfPureSlotCap() {
        return cfPureSlotCap;
    }

    public void setCfPureSlotCap(Integer cfPureSlotCap) {
        this.cfPureSlotCap = cfPureSlotCap;
    }

    public Integer getMmrPoolCap() {
        return mmrPoolCap;
    }

    public void setMmrPoolCap(Integer mmrPoolCap) {
        this.mmrPoolCap = mmrPoolCap;
    }

    public BigDecimal getMmrLambda() {
        return mmrLambda;
    }

    public void setMmrLambda(BigDecimal mmrLambda) {
        this.mmrLambda = mmrLambda;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static RecommendMainConfig defaultConfig() {
        RecommendMainConfig c = new RecommendMainConfig();
        c.setId(1);
        c.setPopWeight(new BigDecimal("0.030000"));
        c.setCfBlendLambda(new BigDecimal("0.250000"));
        c.setCfMaxSeeds(8);
        c.setCfPureSlotCap(4);
        c.setMmrPoolCap(50);
        c.setMmrLambda(new BigDecimal("0.700000"));
        return c;
    }
}
