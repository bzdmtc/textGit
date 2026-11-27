package com.example.smart_learning.exam.dto;

import java.util.List;

public record PracticeOverviewDto(List<PracticeSubjectDto> subjects,
                                  List<PaperDto> papers,
                                  PracticeStatsDto stats,
                                  List<ExamRecordDto> recentRecords) {
}
