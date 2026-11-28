package com.example.smart_learning.university.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MajorRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 32) String code,
        @NotBlank @Pattern(regexp = "学硕|专硕", message = "培养类型只能是学硕/专硕") String degreeType,
        @Size(max = 16) String duration,
        @DecimalMin(value = "0", message = "学费不能为负数") BigDecimal tuition,
        @Size(max = 255) String examSubjects,
        String introduction,
        Integer sort
) {
}
