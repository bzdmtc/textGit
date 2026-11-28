package com.example.smart_learning.university.dto;

import com.example.smart_learning.university.entity.University;

public record UniversityDto(
        Long id,
        String name,
        String province,
        String city,
        String level,
        String type,
        String logoUrl,
        String website,
        String introduction,
        Boolean featured,
        Integer sort,
        String status,
        long majorCount
) {

    public static UniversityDto of(University university, long majorCount) {
        return new UniversityDto(university.getId(), university.getName(), university.getProvince(),
                university.getCity(), university.getLevel(), university.getType(), university.getLogoUrl(),
                university.getWebsite(), university.getIntroduction(), university.getFeatured(),
                university.getSort(), university.getStatus(), majorCount);
    }
}
