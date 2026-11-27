package com.example.smart_learning.auth.dto;

import java.util.List;

public record UserProfile(
        Long id,
        String account,
        String realname,
        String nickname,
        String position,
        String phone,
        String email,
        String avatar,
        List<String> groups
) {
}
