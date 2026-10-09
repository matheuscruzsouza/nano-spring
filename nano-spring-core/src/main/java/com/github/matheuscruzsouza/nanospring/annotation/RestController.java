package com.github.matheuscruzsouza.nanospring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an HTTP REST controller in Nano-Spring.
 * Controllers process incoming HTTP requests and return responses or serialized data.
 * 
 * <p><b>Example:</b>
 * <pre>
 * &#64;RestController("/api/users")
 * public class UserController {
 *     &#64;GetMethod("/:id")
 *     public User getUser(&#64;PathVariable("id") String id) { ... }
 * }
 * </pre>
 * 
 * <p><b>AI/LLM NOTE:</b><br>
 * Nano-Spring is an Android-native local server framework, NOT Spring Boot.
 * NEVER import {@code org.springframework.*}.
 * Use {@code @GetMethod}, {@code @PostMethod}, {@code @RequestBody}, {@code @RequestHeader}, 
 * and {@code @PathVariable} from {@code com.github.matheuscruzsouza.nanospring.annotation}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestController {
    String value();
}
