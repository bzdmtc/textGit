package com.example.smart_learning.auth.dto;

import com.example.smart_learning.permission.dto.MenuItem;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record MeResponse(
        UserProfile user,
        Map<String, Set<String>> rights,
        List<MenuItem> menus,
        String dataScope,
        boolean limited,
        long privVersion
) {
}
