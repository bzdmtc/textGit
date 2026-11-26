package com.example.smart_learning.auth.service;

import com.example.smart_learning.auth.dto.CodeSentResponse;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;

@Service
public class VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final SmsSender smsSender;
    private final EmailCodeSender emailCodeSender;
    private final Duration codeTtl;
    private final Duration sendInterval;
    private final int dailyLimit;
    private final int maxAttempts;
    private final boolean devMode;

    public VerificationCodeService(StringRedisTemplate redis,
                                   SmsSender smsSender,
                                   EmailCodeSender emailCodeSender,
                                   @Value("${app.auth.code-ttl:PT5M}") Duration codeTtl,
                                   @Value("${app.auth.send-interval:PT60S}") Duration sendInterval,
                                   @Value("${app.auth.daily-limit:10}") int dailyLimit,
                                   @Value("${app.auth.max-attempts:5}") int maxAttempts,
                                   @Value("${app.auth.dev-mode:true}") boolean devMode) {
        this.redis = redis;
        this.smsSender = smsSender;
        this.emailCodeSender = emailCodeSender;
        this.codeTtl = codeTtl;
        this.sendInterval = sendInterval;
        this.dailyLimit = dailyLimit;
        this.maxAttempts = maxAttempts;
        this.devMode = devMode;
    }

    public CodeSentResponse sendSms(String phone) {
        return send("sms", phone);
    }

    public CodeSentResponse sendEmail(String email) {
        return send("email", email);
    }

    private CodeSentResponse send(String channel, String target) {
        String sendKey = "auth:send:" + channel + ":" + target;
        Boolean exists = redis.hasKey(sendKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
        String dailyKey = "auth:daily:" + channel + ":" + target + ":" + LocalDate.now();
        Long daily = redis.opsForValue().increment(dailyKey);
        if (daily != null && daily == 1L) {
            redis.expire(dailyKey, Duration.ofDays(1));
        }
        if (daily != null && daily > dailyLimit) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "今日验证码发送次数已达上限");
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redis.opsForValue().set(codeKey(channel, target), code, codeTtl);
        redis.delete(attemptKey(channel, target));
        redis.opsForValue().set(sendKey, "1", sendInterval);

        if ("sms".equals(channel)) {
            smsSender.send(target, code);
        } else {
            emailCodeSender.send(target, code);
        }
        if (devMode) {
            log.info("[DEV] {} verification code for {} is {}", channel, target, code);
        }
        boolean mock = "sms".equals(channel) || emailCodeSender.isMock();
        return new CodeSentResponse((int) codeTtl.toSeconds(), mock);
    }

    public void verify(String channel, String target, String code) {
        String key = codeKey(channel, target);
        String expected = redis.opsForValue().get(key);
        if (expected == null) {
            throw new BusinessException(ErrorCode.CODE_INVALID);
        }
        String attemptKey = attemptKey(channel, target);
        String attemptsRaw = redis.opsForValue().get(attemptKey);
        int attempts = attemptsRaw == null ? 0 : Integer.parseInt(attemptsRaw);
        if (attempts >= maxAttempts) {
            redis.delete(key);
            redis.delete(attemptKey);
            throw new BusinessException(ErrorCode.CODE_INVALID, "验证码错误次数过多，请重新获取");
        }
        if (!expected.equals(code)) {
            Long updated = redis.opsForValue().increment(attemptKey);
            if (updated != null && updated == 1L) {
                redis.expire(attemptKey, codeTtl);
            }
            throw new BusinessException(ErrorCode.CODE_INVALID);
        }
        // Single-use code: consume it after a successful match.
        redis.delete(key);
        redis.delete(attemptKey);
    }

    private String codeKey(String channel, String target) {
        return "auth:code:" + channel + ":" + target;
    }

    private String attemptKey(String channel, String target) {
        return "auth:attempt:" + channel + ":" + target;
    }
}
