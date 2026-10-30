package com.github.matheuscruzsouza.nanospring.openapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indica que um endpoint ou um controller inteiro deve ser ocultado
 * da documentação Swagger / OpenAPI.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Hidden {
}
