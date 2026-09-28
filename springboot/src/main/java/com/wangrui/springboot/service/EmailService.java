package com.wangrui.springboot.service;

import java.util.Map;

public interface EmailService {

    /** 发送邮箱验证码；返回 {code,msg,devCode}。未配置 SMTP 时走开发模式并返回 devCode。 */
    Map<String, Object> sendVerificationCode(String email);

    /** 发送一封测试邮件到指定地址 */
    Map<String, Object> sendTestEmail(String to);
}
