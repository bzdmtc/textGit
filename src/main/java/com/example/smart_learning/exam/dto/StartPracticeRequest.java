package com.example.smart_learning.exam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** 章节练习科目必填；错题重做（source=WRONG）可不传科目，表示全部科目。 */
public record StartPracticeRequest(@Size(max = 32) String subject,
                                   @Size(max = 100) String chapter,
                                   @Min(1) @Max(50) Integer count,
                                   String source) {
}
