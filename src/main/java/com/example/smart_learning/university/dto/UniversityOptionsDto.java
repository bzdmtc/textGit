package com.example.smart_learning.university.dto;

import java.util.List;

public record UniversityOptionsDto(
        List<String> provinces,
        List<String> levels,
        List<String> types
) {
}
