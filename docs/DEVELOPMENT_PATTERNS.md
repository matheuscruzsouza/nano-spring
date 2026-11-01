# Padrões de Desenvolvimento com Nano-Spring 🌱

> **Referência Oficial de Arquitetura, Convenções e Diretrizes de Engenharia para Aplicações Nano-Spring**  
> Destinado a desenvolvedores e agentes de IA para garantir consistência, robustez, segurança e alta performance em ambientes locais e offline-first no Android.

---

## 1. Princípios de Arquitetura (Architecture Principles)

### 1.1 Regra de Ouro: Nano-Spring vs Spring Boot
> [!CAUTION]
> **O NANO-SPRING NÃO É O SPRING BOOT!**  
> - ❌ **Nunca importe pacotes `org.springframework.*`.**  
> - ✅ **Sempre use pacotes `com.github.matheuscruzsouza.nanospring.*`.**  
> - O Nano-Spring é um framework nativo para o Android OS, projetado para operar com baixo overhead de memória e CPU.

### 1.2 Prioridade das Abstrações do Nano-Spring sobre o NanoHTTPD
- O Nano-Spring encapsula internamente o motor HTTP (`NanoHTTPD`).
- ❌ **Não herde de `fi.iki.elonen.NanoHTTPD` nem crie roteamento manual com `session.getUri()`.**
- ✅ **Sempre utilize os recursos nativos do framework:**
  - Inversão de Controle e Injeção de Dependências (`@Service`, `@Repository`, `@Autowired`).
  - Roteamento declarativo (`@RestController`, `@GetMethod`, `@PostMethod`, `@PutMethod`, `@DeleteMethod`).
  - Resolução de parâmetros (`@PathVariable`, `@RequestParam`, `@RequestBody`, `@RequestHeader`).
  - Respostas tipadas via `ResponseEntity<T>` e enums de status `HttpStatus`.
  - Tratamento de exceções com `@ControllerAdvice` e `@ExceptionHandler`.
  - Interceptação com `@Interceptor` (`HandlerInterceptor`).
  - Ciclo de vida gerenciado por `NanoSpringService`.

### 1.3 Separação Arquitetural: Interface do Android vs Interface Web
> [!IMPORTANT]
> **A interface do dispositivo Android e a interface Web para clientes remotos possuem responsabilidades distintas e devem ser desacopladas.**

1. **Interface do Dispositivo Android (`ui/`):**
   - **Opção Preferencial:** Implementada com componentes nativos do Android (**Jetpack Compose** ou layouts **XML / ViewBinding**).
   - **Uso de WebView:** Não é proibida, mas deve ser utilizada **apenas quando houver justificativa técnica clara** (por exemplo, reutilização estrita de dashboards complexos legados com avaliação prévia de memória). Uma UI nativa oferece melhor responsividade e integração com o ciclo de vida do dispositivo.
   - **Papel:** Painel de controle operacional do dispositivo hospedeiro (ligar/desligar servidor, status de IP/porta, métricas locais de bateria, rede e logs).

2. **Interface Web para a Rede Local (`controller/`, `assets/templates/`, `assets/static/`):**
   - **Público:** Clientes remotos na mesma rede Wi-Fi/Ethernet (computadores de mesa, totens, PDVs, navegadores).
   - **Padrão de Renderização:** **HTMX** com templates Mustache dinâmicos via `ModelAndView` (`com.github.matheuscruzsouza.nanospring.ui.ModelAndView`).
   - ❌ **Anti-Pattern:** Nunca retorne strings com HTML inline concatenado dentro dos métodos do controller.
   - **Estáticos:** Arquivos JS (como `htmx.min.js`), CSS e imagens devem residir em `app/src/main/assets/static/` (servidos em `/static/*`).

---

## 2. Convenções de Código & Organização (Coding Conventions)

### 2.1 Estrutura de Pacotes Recomendada
```text
com.example.meuprojeto/
│
├── controller/          # Endpoints HTTP REST e Controllers Web (@RestController)
│   ├── api/
│   │   └── ProductApiController.java
│   └── web/
│       └── ProductWebController.java
│
├── service/             # Regras de Negócio e Background Services
│   ├── AppBackendService.java   # Foreground Service (extende NanoSpringService)
│   └── ProductService.java      # Anotado com @Service
│
├── repository/          # Persistência de Dados (@Repository)
│   └── ProductRepository.java
│
├── dto/                 # Data Transfer Objects (Contratos de Entrada e Saída)
│   ├── request/
│   │   └── CreateProductRequest.java
│   └── response/
│       ├── ErrorResponse.java
│       └── ProductResponse.java
│
├── model/               # Entidades de Domínio
│   └── Product.java
│
├── security/            # Interceptors e Provedores de Autenticação
│   ├── AuthInterceptor.java
│   └── TokenService.java
│
├── exception/           # Tratamento Global de Erros (@ControllerAdvice)
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
│
└── ui/                  # Interface do Usuário Android (Nativa)
    └── MainActivity.java
```

### 2.2 Modelagem de Dados e DTOs
- **Valores Monetários:** Utilize representações exatas (como centavos em `long` ou `BigDecimal`), **evitando `double`/`Double`** para operações financeiras sujeitas a erros de ponto flutuante.
- **Validação de Entrada:** Utilize as anotações do pacote `com.github.matheuscruzsouza.nanospring.annotation.validation.*` (`@NotNull`, `@Size`, `@NotEmpty`, etc.) acopladas ao `@Valid` nos parâmetros `@RequestBody`.

---

## 3. Diretrizes de Segurança (Security Guidelines)

### 3.1 Autenticação Robusta via Interceptor (`@Interceptor`)
O Nano-Spring executa os interceptores antes dos controllers. Se `preHandle(...)` retornar `false`, a requisição é cancelada e o framework retorna `401 Unauthorized`.

#### Regras de Ouro de Segurança:
1. **Validação Rigorosa de Caminhos (Path Matching):** Nunca use `path.startsWith("/api/public")` sem delimitador de segmento de rota (`/`), pois isso liberaria indevidamente caminhos como `/api/public-admin`.
2. **Validação Criptográfica ou Estrutural de Tokens:** Nunca valide tokens verificando apenas prefixo ou tamanho de string. Use um serviço dedicado (`TokenService`) que verifique chave secreta, assinatura, integridade e expiração.
3. **Não libere `/web` indiscriminadamente:** Rotas web administrativas que alteram dados devem ser autenticadas ou protegidas por sessão/token.

```java
package com.example.meuprojeto.security;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.Interceptor;
import com.github.matheuscruzsouza.nanospring.annotation.Order;
import com.github.matheuscruzsouza.nanospring.handler.HandlerInterceptor;
import fi.iki.elonen.NanoHTTPD;

import java.util.Arrays;
import java.util.List;

@Interceptor
@Order(1)
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    // Lista explícita de prefixos públicos com delimitação de barra '/'
    private static final List<String> PUBLIC_PREFIXES = Arrays.asList(
            "/api/public/",
            "/static/",
            "/swagger-ui/"
    );

    @Autowired
    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(NanoHTTPD.IHTTPSession session, String path) {
        // Normaliza o path garantindo formato padrão
        String normalizedPath = path.endsWith("/") ? path : path + "/";

        for (String publicPrefix : PUBLIC_PREFIXES) {
            if (normalizedPath.startsWith(publicPrefix)) {
                return true;
            }
        }

        // Extrai e valida o cabeçalho Authorization
        String authHeader = session.getHeaders().get("authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        String token = authHeader.substring(7).trim();
        return tokenService.validateToken(token);
    }
}
```

---

## 4. Tratamento Centralizado de Exceções (`@ControllerAdvice`)

Para impedir vazamento de detalhes internos, stack traces ou respostas 500 sem corpo JSON explicativo:

```java
package com.example.meuprojeto.exception;

import com.github.matheuscruzsouza.nanospring.annotation.ControllerAdvice;
import com.github.matheuscruzsouza.nanospring.annotation.ExceptionHandler;
import com.github.matheuscruzsouza.nanospring.http.HttpStatus;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;
import com.example.meuprojeto.dto.response.ErrorResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse err = new ErrorResponse(
                404,
                "Not Found",
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        ErrorResponse err = new ErrorResponse(
                400,
                "Bad Request",
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse err = new ErrorResponse(
                500,
                "Internal Server Error",
                "Ocorreu um erro interno no processamento do servidor",
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}
```

---

## 5. Diretrizes do Android Runtime & Ciclo de Vida

O Nano-Spring executa dentro do processo da JVM/ART do Android. O gerenciamento de ciclo de vida deve ser planejado com realismo:

### 5.1 O Papel Real do Foreground Service
- **O que o Foreground Service garante:**
  - Eleva a prioridade do processo perante o Android (*OOM score* prioritário), reduzindo consideravelmente a probabilidade de encerramento prematuro durante uso normal ou em segundo plano.
  - Notificação visível persistente que sinaliza ao usuário que o servidor local está em execução.
- **O que o Foreground Service NÃO garante:**
  - **Não é imune ao Low Memory Killer (LMK):** Se o sistema entrar em estresse severo de memória (ex.: usuário abrindo aplicativo 3D pesado ou câmera em alta resolução), o processo **pode** ser encerrado.
  - **Não substitui tratamento de desconexões de rede:** Mudanças entre Wi-Fi e dados móveis ou suspensões de rede por Doze Mode exigem observação reativa.

### 5.2 Boas Práticas de Resiliência
1. **Configuração de Reinício Automático (`START_STICKY`):** Configure o Foreground Service para retornar `START_STICKY` no `onStartCommand`, permitindo que o Android reinicie o serviço se os recursos forem restaurados.
2. **`foregroundServiceType` Adequado:**
   - Para Android 14+ (`API 34+`), declare tipos condizentes com a atividade da aplicação no `AndroidManifest.xml` (ex.: `dataSync` ou `specialUse` caso justificado).
3. **Prevenção de Doze Mode e Quedas de Rede:**
   - Utilize a trava de multicast (`CHANGE_WIFI_MULTICAST_STATE`) e o WakeLock de forma ponderada e gerencie o desligamento gracioso liberando sockets no `onDestroy`.

---

## 6. Descoberta de Rede Local: mDNS / Zeroconf

Em redes locais móveis (onde o roteador atribui IPs dinâmicos via DHCP), o anúncio mDNS é o padrão recomendado para que clientes na rede acessem o servidor sem necessidade de descobrir o IP manualmente.

### 6.1 Dependência no `app/build.gradle` (ou `build.gradle.kts`)
```kotlin
dependencies {
    val nanoSpringVersion = "2.4.1"
    implementation("com.github.matheuscruzsouza:nano-spring-discovery:$nanoSpringVersion")
}
```

### 6.2 Configuração em `assets/application.properties`
```properties
# Habilita o anúncio de rede mDNS
nano.nsd.enabled=true

# Hostname local anunciado (acessível como http://meu-servidor.local:8080)
nano.nsd.name=meu-servidor

# Tipo do serviço DNS-SD
nano.nsd.type=_http._tcp.

# Habilita o MdnsHostResponder para navegadores web comuns
nano.nsd.host-resolution=true
```

> [!TIP]
> No `AndroidManifest.xml`, certifique-se de incluir `<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />` para que o responder consiga escutar na porta UDP 5353.

---

## 7. Implementação de Referência (Integrada e Consistente)

Abaixo está um conjunto completo e semanticamente consistente demonstrando a integração real entre Repository, Service, Controllers e Injeção de Dependências.

### 7.1 Modelo e DTOs (`model/` e `dto/`)

```java
package com.example.meuprojeto.model;

public class Product {
    private final String id;
    private final String name;
    private final String category;
    private final long priceInCents; // Preço monetário armazenado em centavos

    public Product(String id, String name, String category, long priceInCents) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.priceInCents = priceInCents;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public long getPriceInCents() { return priceInCents; }
}
```

```java
package com.example.meuprojeto.dto.request;

import com.github.matheuscruzsouza.nanospring.annotation.validation.NotNull;
import com.github.matheuscruzsouza.nanospring.annotation.validation.Size;

public class CreateProductRequest {

    @NotNull(message = "O nome é obrigatório")
    @Size(min = 2, max = 80, message = "O nome deve ter entre 2 e 80 caracteres")
    private String name;

    @NotNull(message = "A categoria é obrigatória")
    private String category;

    @NotNull(message = "O preço em centavos é obrigatório")
    private Long priceInCents;

    public CreateProductRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getPriceInCents() { return priceInCents; }
    public void setPriceInCents(Long priceInCents) { this.priceInCents = priceInCents; }
}
```

```java
package com.example.meuprojeto.dto.response;

public class ProductResponse {
    private final String id;
    private final String name;
    private final String category;
    private final long priceInCents;

    public ProductResponse(String id, String name, String category, long priceInCents) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.priceInCents = priceInCents;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public long getPriceInCents() { return priceInCents; }
}
```

### 7.2 Camada de Persistência (`repository/`)
> [!NOTE]
> Quando o módulo `nano-spring-data` está presente, o `DataInitializer` registra a instância gerenciada do `SQLiteDatabase` no mapa de serviços do container IoC do Nano-Spring, tornando-a disponível para injeção via `@Autowired`.

```java
package com.example.meuprojeto.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.Repository;
import com.example.meuprojeto.model.Product;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductRepository {

    private final SQLiteDatabase database;

    @Autowired
    public ProductRepository(SQLiteDatabase database) {
        this.database = database;
    }

    public Product findById(String id) {
        String sql = "SELECT id, name, category, price_in_cents FROM products WHERE id = ?";
        try (Cursor cursor = database.rawQuery(sql, new String[]{id})) {
            if (cursor.moveToFirst()) {
                return mapCursor(cursor);
            }
        }
        return null;
    }

    public List<Product> findAll(String categoryFilter) {
        List<Product> list = new ArrayList<>();
        String sql;
        String[] args;

        if (categoryFilter != null && !categoryFilter.trim().isEmpty()) {
            sql = "SELECT id, name, category, price_in_cents FROM products WHERE category = ?";
            args = new String[]{categoryFilter.trim()};
        } else {
            sql = "SELECT id, name, category, price_in_cents FROM products";
            args = null;
        }

        try (Cursor cursor = database.rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                list.add(mapCursor(cursor));
            }
        }
        return list;
    }

    public void save(Product product) {
        ContentValues values = new ContentValues();
        values.put("id", product.getId());
        values.put("name", product.getName());
        values.put("category", product.getCategory());
        values.put("price_in_cents", product.getPriceInCents());
        database.insertWithOnConflict("products", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private Product mapCursor(Cursor cursor) {
        return new Product(
                cursor.getString(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("category")),
                cursor.getLong(cursor.getColumnIndexOrThrow("price_in_cents"))
        );
    }
}
```

### 7.3 Camada de Negócio (`service/`)

```java
package com.example.meuprojeto.service;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.Service;
import com.example.meuprojeto.dto.request.CreateProductRequest;
import com.example.meuprojeto.dto.response.ProductResponse;
import com.example.meuprojeto.exception.ResourceNotFoundException;
import com.example.meuprojeto.model.Product;
import com.example.meuprojeto.repository.ProductRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse findById(String id) {
        Product entity = productRepository.findById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Produto não encontrado com id: " + id);
        }
        return toResponse(entity);
    }

    public List<ProductResponse> findAll(String categoryFilter) {
        return productRepository.findAll(categoryFilter)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse create(CreateProductRequest request) {
        Product entity = new Product(
                UUID.randomUUID().toString(),
                request.getName(),
                request.getCategory(),
                request.getPriceInCents()
        );
        productRepository.save(entity);
        return toResponse(entity);
    }

    private ProductResponse toResponse(Product entity) {
        return new ProductResponse(
                entity.getId(),
                entity.getName(),
                entity.getCategory(),
                entity.getPriceInCents()
        );
    }
}
```

### 7.4 Controllers (API REST e Interface Web com HTMX)

#### API REST (`ProductApiController.java`):
```java
package com.example.meuprojeto.controller.api;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PathVariable;
import com.github.matheuscruzsouza.nanospring.annotation.PostMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RequestBody;
import com.github.matheuscruzsouza.nanospring.annotation.RequestParam;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.annotation.validation.Valid;
import com.github.matheuscruzsouza.nanospring.http.HttpStatus;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;

import com.example.meuprojeto.dto.request.CreateProductRequest;
import com.example.meuprojeto.dto.response.ProductResponse;
import com.example.meuprojeto.service.ProductService;

import java.util.List;

@RestController("/api/products")
public class ProductApiController {

    private final ProductService productService;

    @Autowired
    public ProductApiController(ProductService productService) {
        this.productService = productService;
    }

    @GetMethod("")
    public ResponseEntity<List<ProductResponse>> listAll(
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(productService.findAll(category));
    }

    @GetMethod("/:id")
    public ResponseEntity<ProductResponse> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMethod("")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

#### Interface Web com HTMX (`ProductWebController.java`):
```java
package com.example.meuprojeto.controller.web;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RequestParam;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.ui.ModelAndView;
import com.example.meuprojeto.service.ProductService;

@RestController("/web/produtos")
public class ProductWebController {

    private final ProductService productService;

    @Autowired
    public ProductWebController(ProductService productService) {
        this.productService = productService;
    }

    // Carrega a página HTML principal em assets/templates/produtos.html
    @GetMethod("")
    public ModelAndView exibirCatalogo() {
        return new ModelAndView("produtos")
                .addObject("titulo", "Catálogo de Produtos")
                .addObject("produtos", productService.findAll(null));
    }

    // Retorna fragmento de template para swap de HTMX em assets/templates/partials/tabela.html
    @GetMethod("/tabela")
    public ModelAndView filtrarPorCategoria(
            @RequestParam(value = "categoria", required = false) String categoria) {
        return new ModelAndView("partials/tabela")
                .addObject("produtos", productService.findAll(categoria));
    }
}
```

---

## 8. Recursos Opcionais (Perguntar Antes ao Desenvolvedor)

> [!NOTE]
> Para evitar complexidade e consumo desnecessário de recursos, certas capacidades são opcionais. **Assistentes de IA e desenvolvedores devem sempre perguntar e validar a necessidade antes de incluí-las:**

| Recurso / Módulo | Quando Perguntar ao Desenvolvedor | Padrão Recomendado se Aprovado |
| :--- | :--- | :--- |
| **Persistência SQLite & Migrações** (`nano-spring-data`) | *"A aplicação precisará de banco de dados SQLite local ou os dados serão mantidos em memória/rede?"* | Se sim: usar `nano-spring-data` com scripts SQL sequenciais (`V1__init.sql`) em `assets/db/migrations/`. |
| **Proteção contra DoS / Rate Limiting** | *"A API será exposta em rede compartilhada exigindo limite de requisições por IP?"* | Se sim: configurar `TokenBucketRateLimiter` no `application.properties`. |
| **CORS e Políticas de Origem** | *"Haverá clientes acessando a API a partir de navegadores externos em origens cruzadas?"* | Se sim: configurar as origens permitidas em `application.properties`. |
| **Documentação Interativa (Swagger / OpenAPI)** | *"Deseja incluir a interface interativa do Swagger (`/swagger-ui/`) para testes locais da API?"* | Se sim: adicionar anotações `@Tag`, `@Operation` e `@ApiResponse` nos controllers. |

---

## 9. Checklist para Novos Projetos

- [ ] Uso exclusivo dos recursos e abstrações do **Nano-Spring** antes de tentar implementar ou importar classes diretas do **NanoHTTPD**.
- [ ] Separação explícita entre a interface do app Android (`ui/`) e a interface Web remota (`controller/`, `assets/`).
- [ ] **Interface do dispositivo preferencialmente nativa** (Compose ou XML); uso de WebView reservado apenas para casos com justificativa técnica e avaliação de memória.
- [ ] Renderização Web remota baseada em **HTMX + templates Mustache** em `assets/templates/` via `ModelAndView`, **sem HTML inline** nos controllers.
- [ ] **Autenticação centralizada com validação de caminho por delimitador (`/`) e validação segura de tokens** (assinatura/integridade/expiração).
- [ ] **Tratamento Centralizado de Exceções** via `@ControllerAdvice` retornando respostas de erro JSON padronizadas (`ErrorResponse`).
- [ ] Registro e anúncio **mDNS / Zeroconf** (`nano-spring-discovery`) com resolução de host habilitada e permissão `CHANGE_WIFI_MULTICAST_STATE`.
- [ ] Resiliência de ciclo de vida planejada (Foreground Service configurado, `START_STICKY` e consciência dos limites do LMK).
- [ ] Alinhamento prévio com o desenvolvedor sobre módulos opcionais (`nano-spring-data`, Swagger, CORS, Rate Limiter).
- [ ] DTOs com validações (`@Valid`, `@NotNull`, `@Size`) e valores monetários modelados sem `Double`.
- [ ] Consultas SQLite protegidas contra SQL Injection via bind parameters (`selectionArgs`).
