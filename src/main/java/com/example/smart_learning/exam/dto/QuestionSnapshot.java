package com.example.smart_learning.exam.dto;

import java.math.BigDecimal;
import java.util.List;

/** 开考/开练时固化的题目快照，题目后续修改不影响历史成绩。 */
public record QuestionSnapshot(Long questionId,
                               String subject,
                               String chapter,
                               String type,
                               String stem,
                               List<QuestionOptionSnapshot> options,
                               String correctAnswer,
                               String analysis,
                               BigDecimal score,
                               Integer sort) {
}
