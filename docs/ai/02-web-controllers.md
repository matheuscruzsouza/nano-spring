# Web Controllers e Roteamento HTTP (Nano-Spring)

O Nano-Spring possui um servidor HTTP embutido e suporta roteamento estilo REST.

## Anotações Principais
Importe SEMPRE de `com.github.matheuscruzsouza.nanospring.annotation.*`.

- `@RestController("caminho-base")`: Define o controller e sua rota base.
- `@GetMethod("caminho")`, `@PostMethod`, `@PutMethod`, `@DeleteMethod`: Define os endpoints.
- `@PathVariable("nome")`: Captura variáveis na rota (`/recurso/:id`). Note a sintaxe `:id`.
- `@RequestBody`: Converte o corpo da requisição de JSON para o objeto.
- `@RequestHeader("Nome")`: Obtém um cabeçalho HTTP.
- `@RequestParam("nome")`: Obtém parâmetros de query string.

## Respostas HTTP
O Nano-Spring possui a classe `ResponseEntity` (não use a do Spring) no pacote `com.github.matheuscruzsouza.nanospring.server.ResponseEntity` (ou similar). 
No entanto, os controllers podem retornar diretamente o objeto, que será serializado para JSON usando Gson.

## Exemplo Completo

```java
import com.github.matheuscruzsouza.nanospring.annotation.*;

@RestController("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // A rota completa será /api/users/:id
    @GetMethod("/:id")
    public User getUser(
            @PathVariable("id") String id,
            @RequestHeader("Authorization") String token) {

        return userService.findById(Integer.parseInt(id));
    }

    // A rota completa será /api/users
    @PostMethod("")
    public ResponseEntity<User> createUser(@RequestBody User newUser) {

        User saved = userService.save(newUser);

        return ResponseEntity
                .created("/api/users/" + saved.getId())
                .body(saved);
    }
}
```
