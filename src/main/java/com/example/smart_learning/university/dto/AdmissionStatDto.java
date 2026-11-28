package com.example.smart_learning.university.dto;

import com.example.smart_learning.university.entity.AdmissionStat;

import java.math.BigDecimal;

public record AdmissionStatDto(
        Long id,
        Long universityId,
        Long majorId,
        String majorName,
        Integer year,
        Integer applicants,
        Integer admitted,
        BigDecimal ratio,
        Integer minScore
) {

    public static AdmissionStatDto of(AdmissionStat stat, String majorName) {
        return new AdmissionStatDto(stat.getId(), stat.getUniversityId(), stat.getMajorId(), majorName,
                stat.getYear(), stat.getApplicants(), stat.getAdmitted(), stat.getRatio(), stat.getMinScore());
    }
}
