package com.github.matheuscruzsouza.nanospring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a business logic service in Nano-Spring.
 * Services are automatically discovered and can be injected into other components.
 * 
 * <p><b>Example:</b>
 * <pre>
 * &#64;Service
 * public class UserService {
 *     private final UserRepository repository;
 *     
 *     public UserService(UserRepository repository) {
 *         this.repository = repository;
 *     }
 * }
 * </pre>
 * 
 * <p><b>AI/LLM NOTE:</b><br>
 * Nano-Spring is a local Android framework, NOT Spring Boot.
 * NEVER import {@code org.springframework.*}.
 * Use Constructor Injection or {@code @Autowired} from {@code com.github.matheuscruzsouza.nanospring.annotation}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {
}
