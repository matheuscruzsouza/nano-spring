# Web Controllers e Roteamento HTTP (Nano-Spring)

O Nano-Spring possui um servidor HTTP embutido e suporta roteamento estilo REST.

## Anotações Principais
Importe SEMPRE de `com.github.matheuscruzsouza.nanospring.annotation.*`.

- `@RestController("caminho-base")`: Define o controller e sua rota base.
- `@GetMethod("caminho")`, `@PostMethod`, `@PutMethod`, `@DeleteMethod`: Define os endpoints.
- `@PathVariable("nome")`: Captura variáveis na rota (`/recurso/:id`). Note a sintaxe `:id`.
- `@RequestBody`: Converte o corpo da requisição (JSON ou `application/x-www-form-urlencoded`) para DTO, `Map<String, String>` ou `String`.
- `@RequestHeader("Nome")`: Obtém um cabeçalho HTTP.
- `@RequestParam("nome")`: Obtém parâmetros de query string ou form-data. Se usado como `@RequestParam Map<String, String>` sem nome, injeta todos os parâmetros.
- `@Authenticated`: Exige autenticação na rota ou em todas as rotas do controller. Suporta Basic Auth ou provedores customizados via `AuthenticationProvider` (ex: Bearer JWT).
- `@RateLimit(requests = 10, durationSeconds = 1)`: Limita a taxa de requisições por IP na rota ou controller (importar de `com.github.matheuscruzsouza.nanospring.security.RateLimit`).

## Respostas HTTP
O Nano-Spring possui a classe `ResponseEntity` (não use a do Spring) no pacote `com.github.matheuscruzsouza.nanospring.http.ResponseEntity`. 
No entanto, os controllers podem retornar diretamente o objeto, que será serializado para JSON usando Gson.

### Download de Arquivos e Streaming de Mídia (`File` e `ResourceRegion`)
Para servir arquivos grandes, vídeos ou áudios com suporte nativo a `Range` (`206 Partial Content`), leitura bufferizada (64 KB) e sem compressão GZIP desnecessária:
- Retorne `java.io.File` diretamente (o MIME type é detectado automaticamente pela extensão do arquivo e inclui `Content-Disposition: attachment`).
- Ou retorne `FileResourceRegion` (`com.github.matheuscruzsouza.nanospring.storage.FileResourceRegion`) para streaming inline no player/navegador.

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
