package com.github.matheuscruzsouza.nanospring.openapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Descreve um parâmetro (path, query, header) no Swagger / OpenAPI.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
public @interface Parameter {
    String name() default "";
    String description() default "";
    boolean required() default false;
    String example() default "";
}
