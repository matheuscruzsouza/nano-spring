package com.github.matheuscruzsouza.nanospring.openapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Descreve uma resposta HTTP de um endpoint no Swagger / OpenAPI.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ApiResponses.class)
public @interface ApiResponse {
    int responseCode() default 200;
    String description() default "";
    Class<?> responseClass() default Void.class;
}
