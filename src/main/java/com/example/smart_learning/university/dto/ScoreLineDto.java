package com.example.smart_learning.university.dto;

import com.example.smart_learning.university.entity.ScoreLine;

public record ScoreLineDto(
        Long id,
        Long universityId,
        Long majorId,
        String majorName,
        Integer year,
        String degreeType,
        Integer political,
        Integer english,
        Integer math,
        Integer majorScore,
        Integer total
) {

    public static ScoreLineDto of(ScoreLine line, String majorName) {
        return new ScoreLineDto(line.getId(), line.getUniversityId(), line.getMajorId(), majorName,
                line.getYear(), line.getDegreeType(), line.getPolitical(), line.getEnglish(), line.getMath(),
                line.getMajorScore(), line.getTotal());
    }
}
