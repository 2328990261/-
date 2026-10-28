package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.EmailConfigMapper;
import com.wangrui.springboot.pojo.EmailConfig;
import com.wangrui.springboot.service.EmailService;
import com.wangrui.springboot.util.EmailCodeStore;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * 邮箱服务实现
 * 负责发送验证码与测试邮件，未配置 SMTP 时走开发模式
 */
@Service
public class EmailServiceImpl implements EmailService {

    // 邮箱格式校验正则
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    // 邮箱配置数据访问对象
    @Autowired
    private EmailConfigMapper emailConfigMapper;

    /**
     * 发送邮箱验证码：校验格式、生成验证码、按配置走开发模式或真实发送
     */
    @Override
    public Map<String, Object> sendVerificationCode(String email) {
        // 1. 初始化返回结果并规范化邮箱
        Map<String, Object> result = new HashMap<>();
        String target = (email == null ? "" : email.trim()).toLowerCase();
        // 2. 校验邮箱格式
        if (!EMAIL_PATTERN.matcher(target).matches()) {
            result.put("code", 400);
            result.put("msg", "邮箱格式不正确");
            return result;
        }
        // 3. 生成6位验证码并暂存
        String code = String.format("%06d", new Random().nextInt(1000000));
        EmailCodeStore.put(target, code);

        // 4. 加载邮箱配置
        EmailConfig cfg = loadConfig();
        // 5. 未配置 SMTP 时按开发模式返回验证码
        if (cfg == null || cfg.getEnabled() == null || cfg.getEnabled() != 1
                || isBlank(cfg.getSmtpHost()) || isBlank(cfg.getUsername()) || isBlank(cfg.getAuthCode())) {
            System.out.println("[开发] 邮箱验证码 " + target + " -> " + code + "（SMTP 未配置，未真正发送）");
            result.put("code", 200);
            result.put("msg", "邮箱服务尚未配置，本次验证码按开发模式返回");
            result.put("devCode", code);
            return result;
        }

        // 6. 真正发送验证码邮件
        try {
            sendMail(cfg, target, "轻小说推荐系统：邮箱验证码",
                    "您好：\n\n您的邮箱验证码是：" + code + "。\n5 分钟内有效，请勿泄露给他人。\n\n（本邮件由系统自动发送，请勿回复）");
            result.put("code", 200);
            result.put("msg", "验证码已发送，请查收邮箱");
            result.put("devCode", null);
        } catch (Exception e) {
            // 7. 发送失败时清除验证码并返回错误
            EmailCodeStore.remove(target);
            result.put("code", 500);
            result.put("msg", "邮件发送失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 发送测试邮件：校验收件邮箱，要求已配置 SMTP，用于验证配置生效
     */
    @Override
    public Map<String, Object> sendTestEmail(String to) {
        // 1. 初始化返回结果并规范化邮箱
        Map<String, Object> result = new HashMap<>();
        String target = (to == null ? "" : to.trim()).toLowerCase();
        // 2. 校验收件邮箱格式
        if (!EMAIL_PATTERN.matcher(target).matches()) {
            result.put("code", 400);
            result.put("msg", "收件邮箱格式不正确");
            return result;
        }
        // 3. 校验 SMTP 配置是否完整
        EmailConfig cfg = loadConfig();
        if (cfg == null || cfg.getEnabled() == null || cfg.getEnabled() != 1
                || isBlank(cfg.getSmtpHost()) || isBlank(cfg.getUsername()) || isBlank(cfg.getAuthCode())) {
            result.put("code", 400);
            result.put("msg", "请先填写并保存邮箱 SMTP 配置（发件邮箱、授权码、启用开关）");
            return result;
        }
        // 4. 发送测试邮件
        try {
            sendMail(cfg, target, "轻小说推荐系统：SMTP 配置测试",
                    "您好：\n\n这是一封测试邮件，说明您的邮箱 SMTP 配置已生效。\n\n（本邮件由系统自动发送，请勿回复）");
            result.put("code", 200);
            result.put("msg", "测试邮件已发送，请查收 " + target);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "测试邮件发送失败：" + e.getMessage());
        }
        return result;
    }

    /** 从数据库加载邮箱 SMTP 配置，ID 固定为 1 */
    private EmailConfig loadConfig() {
        try {
            return emailConfigMapper.selectById(1);
        } catch (Exception e) {
            return null;
        }
    }

    /** 根据配置发送一封纯文本邮件 */
    private void sendMail(EmailConfig cfg, String to, String subject, String text) throws Exception {
        // 1. 构建邮件发送器
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        // 2. 设置SMTP主机
        sender.setHost(cfg.getSmtpHost());
        int port = cfg.getSmtpPort() != null ? cfg.getSmtpPort() : 465;
        sender.setPort(port);
        // 3. 设置发件账号与授权码
        sender.setUsername(cfg.getUsername());
        sender.setPassword(cfg.getAuthCode());
        // 4. 根据SSL开关选择协议
        boolean ssl = cfg.getSslEnabled() != null && cfg.getSslEnabled() == 1;
        sender.setProtocol(ssl ? "smtps" : "smtp");

        // 5. 配置SMTP参数
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }

        // 6. 组装并发送邮件
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(cfg.getUsername());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);
        sender.send(message);
    }

    /** 判断字符串是否为空白 */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}