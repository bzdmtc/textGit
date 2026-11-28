package com.example.smart_learning.university.dto;

import java.util.List;

public record UniversityDetailDto(
        UniversityDto university,
        List<MajorDto> majors,
        List<ScoreLineDto> scoreLines,
        List<AdmissionStatDto> admissionStats
) {
}
