package com.example.smart_learning.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PositionRequest(@NotBlank(message = "职位名称不能为空")
                              @Size(max = 64, message = "职位名称过长")
                              String name,
                              @NotNull(message = "请选择默认权限分组")
                              Long groupId,
                              Integer sort) {
}
