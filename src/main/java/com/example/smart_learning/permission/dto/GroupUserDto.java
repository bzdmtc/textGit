package com.example.smart_learning.permission.dto;

import com.example.smart_learning.user.entity.SysUser;

import java.time.LocalDateTime;

/** 权限分组「用户列表」列展示所需的轻量用户信息。 */
public record GroupUserDto(Long id,
                           String account,
                           String realname,
                           String nickname,
                           String position,
                           String status,
                           LocalDateTime lastLoginAt) {

    public static GroupUserDto of(SysUser user) {
        return new GroupUserDto(user.getId(), user.getAccount(), user.getRealname(), user.getNickname(),
                user.getPosition(), user.getStatus(), user.getLastLoginAt());
    }
}
