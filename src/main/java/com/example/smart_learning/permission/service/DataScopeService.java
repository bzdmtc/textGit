package com.example.smart_learning.permission.service;

import com.example.smart_learning.security.LoginUser;
import com.example.smart_learning.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * 数据范围：ALL 不限，OWN 仅本人负责的数据，SELF 仅本人数据。
 * 课程/题目/文章模块接入时，在查询条件上使用 currentUserId() 或 ownerCondition。
 */
@Service
public class DataScopeService {

    public String currentScope() {
        LoginUser user = requireUser();
        return user.getPermissions().dataScope();
    }

    public boolean isAllScope() {
        return "ALL".equals(currentScope());
    }

    public boolean canAccessOwner(Long ownerUserId) {
        LoginUser user = requireUser();
        if ("ALL".equals(user.getPermissions().dataScope())) {
            return true;
        }
        return ownerUserId != null && ownerUserId.equals(user.getId());
    }

    public void assertOwnerAccess(Long ownerUserId) {
        if (!canAccessOwner(ownerUserId)) {
            throw new AccessDeniedException("无权访问他人数据");
        }
    }

    public Long currentUserId() {
        return requireUser().getId();
    }

    private LoginUser requireUser() {
        LoginUser user = SecurityUtils.currentUser();
        if (user == null) {
            throw new AccessDeniedException("未登录");
        }
        return user;
    }
}
