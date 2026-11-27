package com.example.smart_learning.question.dto;

import com.example.smart_learning.question.entity.QuestionOption;

public record QuestionOptionDto(Long id, String label, String content, Boolean isCorrect, Integer sort) {

    public static QuestionOptionDto of(QuestionOption option) {
        return new QuestionOptionDto(option.getId(), option.getLabel(), option.getContent(),
                option.getIsCorrect(), option.getSort());
    }

    public static QuestionOptionDto publicView(QuestionOption option) {
        return new QuestionOptionDto(option.getId(), option.getLabel(), option.getContent(), null, option.getSort());
    }
}
