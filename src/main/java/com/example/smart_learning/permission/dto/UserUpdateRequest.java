package com.example.smart_learning.permission.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserUpdateRequest(
        @Size(max = 64, message = "姓名过长")
        String realname,
        @Size(max = 64, message = "昵称过长")
        String nickname,
        @Size(max = 64, message = "职位过长")
        String position,
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,
        @Email(message = "邮箱格式不正确")
        String email,
        @Size(max = 500, message = "头像地址过长")
        String avatar,
        /** null 表示不调整分组；传入数组则整组覆盖。 */
        List<Long> groupIds
) {
}
