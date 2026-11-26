package com.example.smart_learning.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArticleRequest(
        Long categoryId,
        @NotBlank(message = "文章标题不能为空")
        @Size(max = 200, message = "文章标题过长")
        String title,
        @Size(max = 500, message = "摘要过长")
        String summary,
        @Size(max = 500, message = "封面地址过长")
        String coverUrl,
        String contentHtml,
        @Size(max = 64, message = "作者过长")
        String author,
        String status
) {
}
