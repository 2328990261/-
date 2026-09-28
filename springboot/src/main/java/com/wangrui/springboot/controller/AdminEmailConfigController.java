package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.EmailConfigMapper;
import com.wangrui.springboot.pojo.EmailConfig;
import com.wangrui.springboot.service.EmailService;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台：邮箱 SMTP 配置（表 email_config，单行 id=1）。QQ 邮箱默认 smtp.qq.com:465(SSL)。
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminEmailConfigController {

    @Autowired
    private EmailConfigMapper emailConfigMapper;

    @Autowired
    private EmailService emailService;

    @GetMapping("/email-config")
    public Result<EmailConfig> get() {
        EmailConfig row;
        try {
            row = emailConfigMapper.selectById(1);
        } catch (Exception e) {
            row = null;
        }
        if (row == null) {
            row = EmailConfig.defaultConfig();
        }
        return Result.success(row);
    }

    @PutMapping("/email-config")
    public Result<Void> save(@RequestBody EmailConfig body) {
        if (body == null) {
            return Result.error("请求体不能为空");
        }
        String host = body.getSmtpHost() == null ? "" : body.getSmtpHost().trim();
        if (host.isEmpty()) {
            return Result.error("SMTP 服务器地址不能为空");
        }
        Integer port = body.getSmtpPort();
        if (port == null || port < 1 || port > 65535) {
            return Result.error("端口号须在 1～65535 之间");
        }
        body.setId(1);
        body.setSmtpHost(host);
        if (body.getSslEnabled() == null) body.setSslEnabled(1);
        if (body.getEnabled() == null) body.setEnabled(0);
        try {
            if (emailConfigMapper.selectById(1) == null) {
                emailConfigMapper.insert(body);
            } else {
                emailConfigMapper.update(body);
            }
        } catch (Exception e) {
            return Result.error("保存失败：请确认已执行 sql 中 email_config 建表脚本。详情：" + e.getMessage());
        }
        return Result.success(null);
    }

    @PostMapping("/email-config/test")
    public Result<Object> test(@RequestBody Map<String, String> request) {
        String to = request != null ? request.get("to") : null;
        Map<String, Object> result = emailService.sendTestEmail(to);
        int code = result.get("code") instanceof Number n ? n.intValue() : 500;
        if (code == 200) {
            return Result.success(result.get("msg"));
        }
        return Result.error(code, String.valueOf(result.get("msg")));
    }
}
