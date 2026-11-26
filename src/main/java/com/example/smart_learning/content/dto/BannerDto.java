package com.example.smart_learning.content.dto;

import com.example.smart_learning.content.entity.Banner;

import java.time.LocalDateTime;

public record BannerDto(
        Long id,
        String title,
        String imageUrl,
        String linkUrl,
        Integer sort,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt
) {

    public static BannerDto of(Banner banner) {
        return new BannerDto(banner.getId(), banner.getTitle(), banner.getImageUrl(), banner.getLinkUrl(),
                banner.getSort(), banner.getStatus(), banner.getStartAt(), banner.getEndAt());
    }
}
