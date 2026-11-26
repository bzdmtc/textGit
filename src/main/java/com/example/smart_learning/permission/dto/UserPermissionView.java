package com.example.smart_learning.permission.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record UserPermissionView(
        Long userId,
        String account,
        Map<String, Set<String>> rights,
        List<MenuItem> menus,
        String dataScope,
        boolean limited,
        long privVersion
) {
}
