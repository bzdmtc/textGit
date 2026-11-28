package com.example.smart_learning.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "手机号或邮箱不能为空")
        String target,
        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
        String code,
        @Size(max = 32, message = "密码长度不能超过 32 位")
        String password,
        @Size(max = 64, message = "昵称长度不能超过 64 位")
        String nickname
) {
}
