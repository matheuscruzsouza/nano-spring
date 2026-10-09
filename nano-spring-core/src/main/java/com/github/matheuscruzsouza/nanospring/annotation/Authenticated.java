package com.github.matheuscruzsouza.nanospring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the endpoint or all endpoints in a controller require authentication.
 * 
 * If applied at the class level, all methods mapped to routes will require authentication.
 * If applied at the method level, only that specific route will require authentication.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authenticated {
}
