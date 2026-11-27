package com.example.smart_learning.learn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank(message = "笔记内容不能为空")
        @Size(max = 2000, message = "笔记不能超过 2000 字")
        String content
) {
}
