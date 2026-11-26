package com.example.smart_learning.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailCodeSender {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeSender.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean mailEnabled;
    private final boolean devMode;

    public EmailCodeSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                           @Value("${app.mail.enabled:false}") boolean mailEnabled,
                           @Value("${app.auth.dev-mode:true}") boolean devMode) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailEnabled = mailEnabled;
        this.devMode = devMode;
    }

    public void send(String email, String code) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || sender == null) {
            log.info("[MOCK EMAIL] email={} code={}", email, code);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("智学考研登录验证码");
        message.setText("你的登录验证码是：" + code + "，5 分钟内有效。若非本人操作请忽略。");
        sender.send(message);
    }

    public boolean isMock() {
        return !mailEnabled || mailSenderProvider.getIfAvailable() == null;
    }

    public boolean isDevMode() {
        return devMode;
    }
}
