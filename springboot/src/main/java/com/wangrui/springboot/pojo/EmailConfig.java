package com.wangrui.springboot.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 表 email_config：邮箱 SMTP 配置（单行 id=1）。QQ 邮箱默认 smtp.qq.com:465(SSL)。
 */
public class EmailConfig {
    private Integer id;
    private String smtpHost;
    private Integer smtpPort;
    private String username;
    private String authCode;
    private Integer sslEnabled; // 0=关 1=开（QQ 邮箱 465 端口需开启）
    private Integer enabled;    // 0=停用 1=启用
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    public Integer getSmtpPort() { return smtpPort; }
    public void setSmtpPort(Integer smtpPort) { this.smtpPort = smtpPort; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }
    public Integer getSslEnabled() { return sslEnabled; }
    public void setSslEnabled(Integer sslEnabled) { this.sslEnabled = sslEnabled; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static EmailConfig defaultConfig() {
        EmailConfig c = new EmailConfig();
        c.setId(1);
        c.setSmtpHost("smtp.qq.com");
        c.setSmtpPort(465);
        c.setUsername("");
        c.setAuthCode("");
        c.setSslEnabled(1);
        c.setEnabled(0);
        return c;
    }
}
