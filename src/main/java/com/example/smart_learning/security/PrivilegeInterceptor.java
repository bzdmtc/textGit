package com.example.smart_learning.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PrivilegeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequiresPriv required = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequiresPriv.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequiresPriv.class);
        }
        if (required == null) {
            return true;
        }
        LoginUser user = SecurityUtils.currentUser();
        if (user == null) {
            throw new AccessDeniedException("未登录");
        }
        if (!user.hasPriv(required.module(), required.method())) {
            throw new AccessDeniedException("缺少权限 " + required.module() + "/" + required.method());
        }
        return true;
    }
}
