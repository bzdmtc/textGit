package com.example.smart_learning.permission.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UserGroupRequest(@NotNull(message = "groupIds 不能为空") List<Long> groupIds) {
}
