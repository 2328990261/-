package com.wangrui.springboot.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表 recommend_score_debug：主推荐路径下调试加分（单行 id=1）。
 */
public class RecommendScoreDebug {
    private Integer id;
    private BigDecimal readRecordUnitBonus;
    private BigDecimal cfCoocUnitBonus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getReadRecordUnitBonus() {
        return readRecordUnitBonus;
    }

    public void setReadRecordUnitBonus(BigDecimal readRecordUnitBonus) {
        this.readRecordUnitBonus = readRecordUnitBonus;
    }

    public BigDecimal getCfCoocUnitBonus() {
        return cfCoocUnitBonus;
    }

    public void setCfCoocUnitBonus(BigDecimal cfCoocUnitBonus) {
        this.cfCoocUnitBonus = cfCoocUnitBonus;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static RecommendScoreDebug defaultRow() {
        RecommendScoreDebug d = new RecommendScoreDebug();
        d.setId(1);
        d.setReadRecordUnitBonus(BigDecimal.ZERO);
        d.setCfCoocUnitBonus(BigDecimal.ZERO);
        return d;
    }
}
