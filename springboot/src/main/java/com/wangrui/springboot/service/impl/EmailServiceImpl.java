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

@Service
public class EmailServiceImpl implements EmailService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    @Autowired
    private EmailConfigMapper emailConfigMapper;

    @Override
    public Map<String, Object> sendVerificationCode(String email) {
        Map<String, Object> result = new HashMap<>();
        String target = (email == null ? "" : email.trim()).toLowerCase();
        if (!EMAIL_PATTERN.matcher(target).matches()) {
            result.put("code", 400);
            result.put("msg", "邮箱格式不正确");
            return result;
        }
        String code = String.format("%06d", new Random().nextInt(1000000));
        EmailCodeStore.put(target, code);

        EmailConfig cfg = loadConfig();
        if (cfg == null || cfg.getEnabled() == null || cfg.getEnabled() != 1
                || isBlank(cfg.getSmtpHost()) || isBlank(cfg.getUsername()) || isBlank(cfg.getAuthCode())) {
            System.out.println("[开发] 邮箱验证码 " + target + " -> " + code + "（SMTP 未配置，未真正发送）");
            result.put("code", 200);
            result.put("msg", "邮箱服务尚未配置，本次验证码按开发模式返回");
            result.put("devCode", code);
            return result;
        }

        try {
            sendMail(cfg, target, "轻小说推荐系统：邮箱验证码",
                    "您好：\n\n您的邮箱验证码是：" + code + "。\n5 分钟内有效，请勿泄露给他人。\n\n（本邮件由系统自动发送，请勿回复）");
            result.put("code", 200);
            result.put("msg", "验证码已发送，请查收邮箱");
            result.put("devCode", null);
        } catch (Exception e) {
            EmailCodeStore.remove(target);
            result.put("code", 500);
            result.put("msg", "邮件发送失败：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> sendTestEmail(String to) {
        Map<String, Object> result = new HashMap<>();
        String target = (to == null ? "" : to.trim()).toLowerCase();
        if (!EMAIL_PATTERN.matcher(target).matches()) {
            result.put("code", 400);
            result.put("msg", "收件邮箱格式不正确");
            return result;
        }
        EmailConfig cfg = loadConfig();
        if (cfg == null || cfg.getEnabled() == null || cfg.getEnabled() != 1
                || isBlank(cfg.getSmtpHost()) || isBlank(cfg.getUsername()) || isBlank(cfg.getAuthCode())) {
            result.put("code", 400);
            result.put("msg", "请先填写并保存邮箱 SMTP 配置（发件邮箱、授权码、启用开关）");
            return result;
        }
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

    private EmailConfig loadConfig() {
        try {
            return emailConfigMapper.selectById(1);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendMail(EmailConfig cfg, String to, String subject, String text) throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(cfg.getSmtpHost());
        int port = cfg.getSmtpPort() != null ? cfg.getSmtpPort() : 465;
        sender.setPort(port);
        sender.setUsername(cfg.getUsername());
        sender.setPassword(cfg.getAuthCode());
        boolean ssl = cfg.getSslEnabled() != null && cfg.getSslEnabled() == 1;
        sender.setProtocol(ssl ? "smtps" : "smtp");

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

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(cfg.getUsername());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);
        sender.send(message);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
