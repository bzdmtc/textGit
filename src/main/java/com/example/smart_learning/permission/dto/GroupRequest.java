package com.example.smart_learning.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GroupRequest(
        @NotBlank(message = "分组编码不能为空")
        @Size(max = 64, message = "分组编码过长")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "分组编码只能包含字母、数字、下划线和短横线")
        String code,
        @NotBlank(message = "分组名称不能为空")
        @Size(max = 128, message = "分组名称过长")
        String name,
        @Size(max = 500, message = "分组描述过长")
        String description,
        @Pattern(regexp = "ALL|OWN|SELF", message = "数据范围只能是 ALL/OWN/SELF")
        String dataScope,
        Boolean limited,
        String scopeType,
        Long scopeId,
        Integer sort
) {
}
