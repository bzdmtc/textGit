package com.example.smart_learning.permission.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserCreateRequest(
        @NotBlank(message = "账号不能为空")
        @Size(max = 64, message = "账号过长")
        @Pattern(regexp = "[A-Za-z0-9_.-]+", message = "账号只能包含字母、数字、点、下划线和短横线")
        String account,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需在 6-64 位之间")
        String password,
        @Size(max = 64, message = "姓名过长")
        String realname,
        @Size(max = 64, message = "昵称过长")
        String nickname,
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,
        @Email(message = "邮箱格式不正确")
        String email,
        List<Long> groupIds
) {
}
