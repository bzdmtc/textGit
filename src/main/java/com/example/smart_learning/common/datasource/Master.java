package com.example.smart_learning.common.datasource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Force the method to use the write (master) database, even inside a read-only transaction.
 * Used for security-critical reads such as login, permissions and payment status.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Master {
}
