package com.example.smart_learning.content.dto;

import com.example.smart_learning.content.entity.Article;

import java.time.LocalDateTime;

public record ArticleDto(
        Long id,
        Long categoryId,
        String categoryName,
        String title,
        String summary,
        String coverUrl,
        String contentHtml,
        String author,
        String status,
        LocalDateTime publishedAt,
        Integer viewCount,
        Long ownerUserId,
        LocalDateTime createdAt
) {

    public static ArticleDto of(Article article, String categoryName) {
        return new ArticleDto(article.getId(), article.getCategoryId(), categoryName, article.getTitle(),
                article.getSummary(), article.getCoverUrl(), article.getContentHtml(), article.getAuthor(),
                article.getStatus(), article.getPublishedAt(), article.getViewCount(), article.getOwnerUserId(),
                article.getCreatedAt());
    }
}
