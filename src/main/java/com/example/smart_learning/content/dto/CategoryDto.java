package com.example.smart_learning.content.dto;

import com.example.smart_learning.content.entity.ArticleCategory;

public record CategoryDto(Long id, String name, Integer sort) {

    public static CategoryDto of(ArticleCategory category) {
        return new CategoryDto(category.getId(), category.getName(), category.getSort());
    }
}
