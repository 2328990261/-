package com.wangrui.springboot.pojo;

/**
 * 用户标签权重（运行时计算，用于展示与推荐解释，不落库）。
 */
public class UserTagWeight {
    private String tagName;
    /** 归一化后的权重（0~1，总和约等于1） */
    private double weight;
    /** 原始分（未归一化） */
    private double raw;
    /** 是否为用户手动配置（覆盖项） */
    private boolean overridden;

    public UserTagWeight() {}

    public UserTagWeight(String tagName, double weight, double raw) {
        this.tagName = tagName;
        this.weight = weight;
        this.raw = raw;
        this.overridden = false;
    }

    public UserTagWeight(String tagName, double weight, double raw, boolean overridden) {
        this.tagName = tagName;
        this.weight = weight;
        this.raw = raw;
        this.overridden = overridden;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getRaw() {
        return raw;
    }

    public void setRaw(double raw) {
        this.raw = raw;
    }

    public boolean isOverridden() {
        return overridden;
    }

    public void setOverridden(boolean overridden) {
        this.overridden = overridden;
    }
}

