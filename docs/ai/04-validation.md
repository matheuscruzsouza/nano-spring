# Validação Declarativa (Nano-Spring)

O Nano-Spring possui seu próprio motor de validação baseado em anotações.
NÃO use `javax.validation` ou `jakarta.validation`.
Use sempre as anotações do pacote: `com.github.matheuscruzsouza.nanospring.validation.*`.

## Anotações Disponíveis
- `@Valid`
- `@NotNull`
- `@NotBlank`
- `@Size`
- `@Min`
- `@Max`
- `@Email`
- `@Pattern`

## Exemplo em DTOs

```java
import com.github.matheuscruzsouza.nanospring.validation.*;

public class UserDTO {

    @NotNull
    @NotBlank
    private String name;

    @Email
    private String email;

    @Min(18)
    @Max(120)
    private int age;
    
    // Getters e Setters...
}
```

## Validação no Controller
Use `@Valid` junto com `@RequestBody` para validar automaticamente a entrada.
```java
import com.github.matheuscruzsouza.nanospring.annotation.*;
import com.github.matheuscruzsouza.nanospring.validation.Valid;

@RestController("/api/users")
public class UserController {

    @PostMethod("")
    public ResponseEntity<?> create(@Valid @RequestBody UserDTO user) {
        // Se a validação falhar, o Nano-Spring retorna erro HTTP 400 automaticamente
        return ResponseEntity.ok(userService.save(user));
    }
}
```
