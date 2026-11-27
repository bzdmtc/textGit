package com.example.smart_learning.exam.dto;

import java.util.List;

public record PracticeSubjectDto(String subject, int questionCount, List<PracticeChapterDto> chapters) {
}
