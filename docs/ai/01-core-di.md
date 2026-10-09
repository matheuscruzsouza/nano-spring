# Injeção de Dependência no Nano-Spring

O Nano-Spring possui um mecanismo próprio de DI baseado em reflexão executada no momento do startup.

## Anotações Principais
Importe SEMPRE de `com.github.matheuscruzsouza.nanospring.annotation.*`.

- `@Service`: Registra uma classe contendo lógica de negócio.
- `@Repository`: Registra uma classe de persistência (ex: banco de dados).
- `@RestController`: Registra um controller HTTP.
- `@Autowired`: Marca uma dependência para injeção.
- `@Value`: Injeta valores de propriedades (`application.properties`).

## Exemplos de Uso

### Criação de um Service
```java
import com.github.matheuscruzsouza.nanospring.annotation.Service;

@Service
public class UserService {
    // Lógica aqui
}
```

### Injeção de Dependência via Construtor (Recomendado)
```java
import com.github.matheuscruzsouza.nanospring.annotation.Service;

@Service
public class OrderService {
    private final UserService userService;

    public OrderService(UserService userService) {
        this.userService = userService;
    }
}
```

### Injeção via @Autowired (Propriedade)
```java
import com.github.matheuscruzsouza.nanospring.annotation.Service;
import com.github.matheuscruzsouza.nanospring.annotation.Autowired;

@Service
public class ReportService {
    
    @Autowired
    private UserService userService;
}
```

### Injeção de Configuração (@Value)
```java
import com.github.matheuscruzsouza.nanospring.annotation.Service;
import com.github.matheuscruzsouza.nanospring.annotation.Value;

@Service
public class ConfigService {
    
    @Value("${api.key:default-key}")
    private String apiKey;
}
```
