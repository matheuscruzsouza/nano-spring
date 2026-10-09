package com.github.matheuscruzsouza.nanospring.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a property, method parameter or field for validation.
 * Nano-Spring provides its own validation engine based on annotations.
 * 
 * <p><b>Example:</b>
 * <pre>
 * &#64;PostMethod("/api/users")
 * public ResponseEntity&lt;?&gt; create(&#64;Valid &#64;RequestBody UserDTO user) { ... }
 * </pre>
 * 
 * <p><b>AI/LLM NOTE:</b><br>
 * Nano-Spring is a local Android framework, NOT Spring Boot.
 * NEVER import {@code javax.validation.*} or {@code jakarta.validation.*}.
 * Use the annotations from {@code com.github.matheuscruzsouza.nanospring.validation.*} 
 * such as {@code @NotNull}, {@code @NotBlank}, {@code @Email}, {@code @Min}, and {@code @Max}.
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Valid {
}
