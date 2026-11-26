package com.example.smart_learning.content.dto;

import com.example.smart_learning.content.entity.Material;

public record MaterialDto(
        Long id,
        String title,
        String subject,
        String fileUrl,
        Long fileSize,
        Integer downloadCount,
        String status,
        Long ownerUserId
) {

    public static MaterialDto of(Material material) {
        return new MaterialDto(material.getId(), material.getTitle(), material.getSubject(), material.getFileUrl(),
                material.getFileSize(), material.getDownloadCount(), material.getStatus(), material.getOwnerUserId());
    }
}
