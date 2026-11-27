package com.example.smart_learning.exam.dto;

import java.util.List;

public record PaperDetailDto(PaperDto paper, List<PaperQuestionDto> questions) {
}
