package com.example.smart_learning.permission.dto;

import com.example.smart_learning.permission.entity.SysGroup;
import com.example.smart_learning.permission.entity.SysPosition;

public record PositionDto(Long id,
                          String name,
                          Long groupId,
                          String groupCode,
                          String groupName,
                          String status,
                          Integer sort) {

    public static PositionDto of(SysPosition position, SysGroup group) {
        return new PositionDto(position.getId(), position.getName(), position.getGroupId(),
                group == null ? null : group.getCode(),
                group == null ? null : group.getName(),
                position.getStatus(), position.getSort());
    }
}
