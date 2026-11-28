package com.example.smart_learning.auth.controller;

import com.example.smart_learning.auth.dto.CaptchaResponse;
import com.example.smart_learning.auth.dto.CodeSentResponse;
import com.example.smart_learning.auth.dto.EmailCodeRequest;
import com.example.smart_learning.auth.dto.EmailLoginRequest;
import com.example.smart_learning.auth.dto.LoginRequest;
import com.example.smart_learning.auth.dto.MeResponse;
import com.example.smart_learning.auth.dto.RefreshRequest;
import com.example.smart_learning.auth.dto.RegisterRequest;
import com.example.smart_learning.auth.dto.SmsCodeRequest;
import com.example.smart_learning.auth.dto.SmsLoginRequest;
import com.example.smart_learning.auth.dto.TokenResponse;
import com.example.smart_learning.auth.service.AuthService;
import com.example.smart_learning.auth.service.AuthThrottleService;
import com.example.smart_learning.auth.service.CaptchaService;
import com.example.smart_learning.auth.service.VerificationCodeService;
import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.security.LoginUser;
import com.example.smart_learning.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /** IP 级限流：防止换账号/换目标继续爆破。 */
    private static final Duration MINUTE = Duration.ofMinutes(1);

    private final AuthService authService;
    private final VerificationCodeService verificationCodeService;
    private final CaptchaService captchaService;
    private final AuthThrottleService throttleService;

    public AuthController(AuthService authService,
                          VerificationCodeService verificationCodeService,
                          CaptchaService captchaService,
                          AuthThrottleService throttleService) {
        this.authService = authService;
        this.verificationCodeService = verificationCodeService;
        this.captchaService = captchaService;
        this.throttleService = throttleService;
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha(HttpServletRequest http) {
        throttleService.check("captcha", clientIp(http), 60, MINUTE);
        return ApiResponse.success(captchaService.create());
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        throttleService.check("login", clientIp(http), 30, MINUTE);
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/sms/code")
    public ApiResponse<CodeSentResponse> sendSmsCode(@Valid @RequestBody SmsCodeRequest request, HttpServletRequest http) {
        throttleService.check("sms-code", clientIp(http), 10, MINUTE);
        return ApiResponse.success(verificationCodeService.sendSms(request.phone()));
    }

    @PostMapping("/sms/login")
    public ApiResponse<TokenResponse> loginBySms(@Valid @RequestBody SmsLoginRequest request, HttpServletRequest http) {
        throttleService.check("sms-login", clientIp(http), 20, MINUTE);
        return ApiResponse.success(authService.loginBySms(request));
    }

    @PostMapping("/register")
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        throttleService.check("register", clientIp(http), 10, MINUTE);
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/email/code")
    public ApiResponse<CodeSentResponse> sendEmailCode(@Valid @RequestBody EmailCodeRequest request, HttpServletRequest http) {
        throttleService.check("email-code", clientIp(http), 10, MINUTE);
        return ApiResponse.success(verificationCodeService.sendEmail(request.email()));
    }

    @PostMapping("/email/login")
    public ApiResponse<TokenResponse> loginByEmail(@Valid @RequestBody EmailLoginRequest request, HttpServletRequest http) {
        throttleService.check("email-login", clientIp(http), 20, MINUTE);
        return ApiResponse.success(authService.loginByEmail(request));
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        LoginUser user = SecurityUtils.currentUser();
        if (user != null) {
            authService.logout(user.getId());
        }
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me() {
        LoginUser user = SecurityUtils.currentUser();
        if (user == null) {
            return ApiResponse.error(20001, "未登录");
        }
        return ApiResponse.success(authService.me(user));
    }
}
