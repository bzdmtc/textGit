package com.example.smart_learning.auth.controller;

import com.example.smart_learning.auth.dto.CodeSentResponse;
import com.example.smart_learning.auth.dto.EmailCodeRequest;
import com.example.smart_learning.auth.dto.EmailLoginRequest;
import com.example.smart_learning.auth.dto.LoginRequest;
import com.example.smart_learning.auth.dto.MeResponse;
import com.example.smart_learning.auth.dto.RefreshRequest;
import com.example.smart_learning.auth.dto.SmsCodeRequest;
import com.example.smart_learning.auth.dto.SmsLoginRequest;
import com.example.smart_learning.auth.dto.TokenResponse;
import com.example.smart_learning.auth.service.AuthService;
import com.example.smart_learning.auth.service.VerificationCodeService;
import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.security.LoginUser;
import com.example.smart_learning.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final VerificationCodeService verificationCodeService;

    public AuthController(AuthService authService, VerificationCodeService verificationCodeService) {
        this.authService = authService;
        this.verificationCodeService = verificationCodeService;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/sms/code")
    public ApiResponse<CodeSentResponse> sendSmsCode(@Valid @RequestBody SmsCodeRequest request) {
        return ApiResponse.success(verificationCodeService.sendSms(request.phone()));
    }

    @PostMapping("/sms/login")
    public ApiResponse<TokenResponse> loginBySms(@Valid @RequestBody SmsLoginRequest request) {
        return ApiResponse.success(authService.loginBySms(request));
    }

    @PostMapping("/email/code")
    public ApiResponse<CodeSentResponse> sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        return ApiResponse.success(verificationCodeService.sendEmail(request.email()));
    }

    @PostMapping("/email/login")
    public ApiResponse<TokenResponse> loginByEmail(@Valid @RequestBody EmailLoginRequest request) {
        return ApiResponse.success(authService.loginByEmail(request));
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
