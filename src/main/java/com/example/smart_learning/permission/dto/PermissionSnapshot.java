package com.example.smart_learning.permission.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record PermissionSnapshot(
        long privVersion,
        Map<String, Set<String>> rights,
        List<MenuItem> menus,
        List<String> groupCodes,
        String dataScope,
        boolean limited
) {

    public boolean has(String module, String method) {
        Set<String> methods = rights.get(module);
        return methods != null && methods.contains(method);
    }
}
