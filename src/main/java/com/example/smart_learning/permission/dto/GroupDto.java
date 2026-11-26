package com.example.smart_learning.permission.dto;

import com.example.smart_learning.permission.entity.SysGroup;

public record GroupDto(
        Long id,
        String code,
        String name,
        String role,
        String dataScope,
        boolean limited,
        String scopeType,
        Long scopeId,
        String status,
        int sort,
        long userCount
) {

    public static GroupDto of(SysGroup group, long userCount) {
        return new GroupDto(group.getId(), group.getCode(), group.getName(), group.getRole(),
                group.getDataScope(), Boolean.TRUE.equals(group.getLimited()), group.getScopeType(),
                group.getScopeId(), group.getStatus(), group.getSort(), userCount);
    }
}
