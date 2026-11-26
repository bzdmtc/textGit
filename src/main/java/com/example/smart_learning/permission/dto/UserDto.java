package com.example.smart_learning.permission.dto;

import com.example.smart_learning.user.entity.SysUser;

import java.time.LocalDateTime;
import java.util.List;

public record UserDto(
        Long id,
        String account,
        String realname,
        String nickname,
        String phone,
        String email,
        String avatar,
        String status,
        Long privVersion,
        LocalDateTime lastLoginAt,
        List<GroupDto> groups
) {

    public static UserDto of(SysUser user, List<GroupDto> groups) {
        return new UserDto(user.getId(), user.getAccount(), user.getRealname(), user.getNickname(),
                user.getPhone(), user.getEmail(), user.getAvatar(), user.getStatus(),
                user.getPrivVersion(), user.getLastLoginAt(), groups);
    }
}
