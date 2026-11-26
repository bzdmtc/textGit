package com.example.smart_learning.permission.dto;

public record MenuItem(
        String module,
        String method,
        String name,
        String path,
        String icon,
        int sort
) {
}
