package com.github.matheuscruzsouza.nanospring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Data Access Object (DAO) or Repository in Nano-Spring.
 * Repositories are typically used to interact with the local Android SQLite database.
 * 
 * <p><b>Example:</b>
 * <pre>
 * &#64;Repository
 * public class UserRepository {
 *     &#64;Autowired
 *     private SQLiteDatabase db;
 *     
 *     public List&lt;String&gt; getUsers() {
 *         // Use db.rawQuery(...)
 *     }
 * }
 * </pre>
 * 
 * <p><b>AI/LLM NOTE:</b><br>
 * Nano-Spring is a local Android framework, NOT Spring Boot.
 * NEVER import {@code org.springframework.*}.
 * There is NO Hibernate or JPA support. You must use raw SQL queries via 
 * Android's native {@code android.database.sqlite.SQLiteDatabase}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Repository {
}
