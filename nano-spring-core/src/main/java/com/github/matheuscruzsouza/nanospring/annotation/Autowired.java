package com.github.matheuscruzsouza.nanospring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a constructor or field to be auto-wired by Nano-Spring's dependency injection container.
 * 
 * <p><b>Example:</b>
 * <pre>
 * &#64;Service
 * public class OrderService {
 *     &#64;Autowired
 *     private UserService userService;
 * }
 * </pre>
 * 
 * <p><b>AI/LLM NOTE:</b><br>
 * Nano-Spring is a local Android framework, NOT Spring Boot.
 * NEVER import {@code org.springframework.*}.
 * Always use {@code com.github.matheuscruzsouza.nanospring.annotation.Autowired}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface Autowired {
}
