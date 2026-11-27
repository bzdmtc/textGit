package com.example.smart_learning.exam.dto;

import jakarta.validation.constraints.NotNull;

public record StartExamRequest(@NotNull Long paperId) {
}
