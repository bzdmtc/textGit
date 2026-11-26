package com.example.smart_learning.common.datasource;

import java.util.function.Supplier;

public final class DataSourceContextHolder {

    private static final ThreadLocal<Route> ROUTE = new ThreadLocal<>();

    private DataSourceContextHolder() {
    }

    public static void setMaster() {
        ROUTE.set(Route.MASTER);
    }

    public static void setRead() {
        ROUTE.set(Route.READ);
    }

    public static Route current() {
        return ROUTE.get();
    }

    public static void clear() {
        ROUTE.remove();
    }

    public static <T> T master(Supplier<T> action) {
        return withRoute(Route.MASTER, action);
    }

    public static void master(Runnable action) {
        withRoute(Route.MASTER, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T read(Supplier<T> action) {
        return withRoute(Route.READ, action);
    }

    public static <T> T withRoute(Route route, Supplier<T> action) {
        Route previous = ROUTE.get();
        ROUTE.set(route);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                ROUTE.remove();
            } else {
                ROUTE.set(previous);
            }
        }
    }
}
