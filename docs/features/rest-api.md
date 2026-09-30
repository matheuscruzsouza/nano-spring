# 🌐 Construindo APIs RESTful

O **Nano-Spring** oferece um conjunto de anotações expressivas e intuitivas para construção de endpoints REST, compatível com o estilo de desenvolvimento do Spring Boot.

---

## 📌 Anotações Principais

| Anotação | Alvo | Descrição |
| :--- | :--- | :--- |
| `@RestController(prefix)` | Classe | Registra a classe como um controlador REST e define o prefixo base da rota |
| `@GetMethod(path)` | Método | Mapeia requisições `GET` |
| `@PostMethod(path)` | Método | Mapeia requisições `POST` |
| `@PutMethod(path)` | Método | Mapeia requisições `PUT` |
| `@DeleteMethod(path)` | Método | Mapeia requisições `DELETE` |
| `@PathVariable(name)` | Parâmetro | Extrai valores da rota (ex: `/usuarios/:id`) |
| `@RequestParam(name)` | Parâmetro | Extrai valores de Query String (ex: `?filtro=ativo`) |
| `@RequestBody` | Parâmetro | Desserializa automaticamente o corpo JSON da requisição em um POJO |
| `@ResponseStatus` | Método | Define o código de status HTTP padrão da resposta |

---

## 💡 Exemplos Práticos

### 1. CRUD Completo de Usuários

```java
package com.exemplo.meuapp.controllers;

import com.github.matheuscruzsouza.nanospring.annotation.*;
import com.github.matheuscruzsouza.nanospring.http.HttpStatus;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;
import com.exemplo.meuapp.model.User;
import com.exemplo.meuapp.service.UserService;

import java.util.List;

@RestController("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users ou /api/users?status=ativo
    @GetMethod("")
    public List<User> listUsers(@RequestParam(value = "status", required = false) String status) {
        if (status != null) {
            return userService.findByStatus(status);
        }
        return userService.findAll();
    }

    // GET /api/users/42
    @GetMethod("/:id")
    public ResponseEntity<User> getUserById(@PathVariable("id") Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(user);
    }

    // POST /api/users (Recebe JSON e devolve 201 CREATED)
    @PostMethod("")
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody User newUser) {
        return userService.save(newUser);
    }

    // PUT /api/users/42
    @PutMethod("/:id")
    public ResponseEntity<User> updateUser(@PathVariable("id") Long id, @RequestBody User user) {
        User updated = userService.update(id, user);
        return ResponseEntity.ok(updated);
    }

    // DELETE /api/users/42
    @DeleteMethod("/:id")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        userService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
```

---

## ⚙️ Uso de `ResponseEntity` e `HttpStatus`

A classe `ResponseEntity<T>` permite controle total sobre o status HTTP e cabeçalhos enviados:

```java
import com.github.matheuscruzsouza.nanospring.http.HttpStatus;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;

@GetMethod("/custom")
public ResponseEntity<String> customResponse() {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
            .header("X-Custom-Header", "NanoSpringAndroid")
            .body("Operação em processamento");
}
```

---

## 🔍 Conversão Automática de JSON

Quando um método retorna qualquer objeto Java (POJO, `Map`, `List`):
* O framework define automaticamente o `Content-Type: application/json; charset=utf-8`.
* O objeto é serializado em JSON utilizando o `Gson`.
* Se o método retornar uma `String`, ela é entregue como texto plano (ou HTML/XML se especificado `mimeType`).

[⬅ Voltar para o Índice](../index.md)
