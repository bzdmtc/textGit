package com.example.smart_learning.config;

import com.example.smart_learning.common.datasource.DataSourceContextHolder;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.datasource.ReadOnly;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataSourceRoutingAspect {

    @Pointcut("@annotation(com.example.smart_learning.common.datasource.ReadOnly)")
    public void readOnlyMethod() {
    }

    @Pointcut("@within(com.example.smart_learning.common.datasource.ReadOnly)")
    public void readOnlyClass() {
    }

    @Pointcut("@annotation(com.example.smart_learning.common.datasource.Master)")
    public void masterMethod() {
    }

    @Pointcut("@within(com.example.smart_learning.common.datasource.Master)")
    public void masterClass() {
    }

    @Around("readOnlyMethod() || readOnlyClass()")
    public Object routeRead(ProceedingJoinPoint joinPoint) throws Throwable {
        return DataSourceContextHolder.withRoute(com.example.smart_learning.common.datasource.Route.READ, () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                if (throwable instanceof RuntimeException runtime) {
                    throw runtime;
                }
                if (throwable instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException(throwable);
            }
        });
    }

    @Around("masterMethod() || masterClass()")
    public Object routeMaster(ProceedingJoinPoint joinPoint) throws Throwable {
        return DataSourceContextHolder.withRoute(com.example.smart_learning.common.datasource.Route.MASTER, () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                if (throwable instanceof RuntimeException runtime) {
                    throw runtime;
                }
                if (throwable instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException(throwable);
            }
        });
    }
}
