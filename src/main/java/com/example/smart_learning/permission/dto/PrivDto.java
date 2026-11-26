package com.example.smart_learning.permission.dto;

import com.example.smart_learning.permission.entity.SysPriv;

public record PrivDto(
        Long id,
        String module,
        String method,
        String name,
        String type,
        Long parentId,
        String menuPath,
        String icon,
        int sort,
        String status
) {

    public static PrivDto of(SysPriv priv) {
        return new PrivDto(priv.getId(), priv.getModule(), priv.getMethod(), priv.getName(),
                priv.getType(), priv.getParentId(), priv.getMenuPath(), priv.getIcon(),
                priv.getSort(), priv.getStatus());
    }
}
