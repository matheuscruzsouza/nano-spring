package com.github.matheuscruzsouza.nanospring.annotation;

import com.github.matheuscruzsouza.nanospring.http.HttpStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseStatus {
    HttpStatus value() default HttpStatus.INTERNAL_SERVER_ERROR;
    int code() default 0;
    String reason() default "";
}
