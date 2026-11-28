package com.example.smart_learning.university.dto;

import com.example.smart_learning.university.entity.UniversityMajor;

import java.math.BigDecimal;

public record MajorDto(
        Long id,
        Long universityId,
        String name,
        String code,
        String degreeType,
        String duration,
        BigDecimal tuition,
        String examSubjects,
        String introduction,
        Integer sort,
        String status
) {

    public static MajorDto of(UniversityMajor major) {
        return new MajorDto(major.getId(), major.getUniversityId(), major.getName(), major.getCode(),
                major.getDegreeType(), major.getDuration(), major.getTuition(), major.getExamSubjects(),
                major.getIntroduction(), major.getSort(), major.getStatus());
    }
}
