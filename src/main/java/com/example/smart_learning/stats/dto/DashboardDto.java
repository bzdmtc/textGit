package com.example.smart_learning.stats.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardDto(
        int days,
        LocalDate today,
        SummaryDto summary,
        List<TrendPointDto> trend,
        List<HotCourseDto> hotCourses,
        List<NamedCountDto> orderStatus
) {
}
