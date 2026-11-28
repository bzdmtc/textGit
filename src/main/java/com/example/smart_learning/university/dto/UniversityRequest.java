package com.example.smart_learning.university.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UniversityRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 32) String province,
        @Size(max = 32) String city,
        @Size(max = 32) String level,
        @Size(max = 32) String type,
        @Size(max = 500) String logoUrl,
        @Size(max = 255) String website,
        String introduction,
        Boolean featured,
        Integer sort
) {
}
