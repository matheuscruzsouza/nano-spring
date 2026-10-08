package com.github.matheuscruzsouza.nanospring.openapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Descreve a operação e propósito de um endpoint REST no Swagger / OpenAPI.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Operation {
    String summary() default "";
    String description() default "";
    boolean deprecated() default false;
}
