# Padrões de Desenvolvimento com Nano-Spring 🌱

> **Guia de Arquitetura, Boas Práticas e Padrões de Projeto para Aplicações Android com Nano-Spring**  
> Destinado a desenvolvedores e agentes de IA para garantir consistência, robustez e alta performance em ambientes locais/offline-first.

---

## 1. Regra Fundamental: Nano-Spring vs Spring Boot & NanoHTTPD

> [!CAUTION]
> **1. O Nano-Spring NÃO é o Spring Boot:**  
> - ❌ **Nunca importe pacotes `org.springframework.*`.**  
> - ✅ **Sempre use pacotes `com.github.matheuscruzsouza.nanospring.*`.**  
> - O Nano-Spring é um framework concebido nativamente para o Android OS, projetado para transformar dispositivos móveis/embarcados em servidores HTTP e nós de processamento locais.
>
> **2. Priorize SEMPRE as funcionalidades do Nano-Spring antes do NanoHTTPD:**  
> - O Nano-Spring já encapsula e abstrai o motor HTTP interno (`NanoHTTPD`).
> - ❌ **Não instancie nem herde diretamente classes do `fi.iki.elonen.NanoHTTPD`** para criar servidores, roteadores ou endpoints manuais.
> - ❌ **Não manipule sockets de baixo nível ou crie parsers manuais** quando o framework já possui anotações de alto nível.
> - ✅ **Sempre utilize os recursos nativos do framework:**
>   - Controllers anotados (`@RestController`, `@GetMethod`, `@PostMethod`, `@PutMethod`, `@DeleteMethod`).
>   - Resolução automática de parâmetros (`@PathVariable`, `@RequestParam`, `@RequestBody`, `@RequestHeader`).
>   - Respostas estruturadas via `ResponseEntity<T>` e `HttpStatus`.
>   - Inversão de controle e injeção de dependências (`@Service`, `@Repository`, `@Autowired`).
>   - Gerenciamento de ciclo de vida e background via `NanoSpringService`.
>   - Renderização web via `ModelAndView` e streaming reativo via `SseEmitter`.
> - Só utilize classes internas ou APIs de baixo nível se houver uma necessidade estrita de extensibilidade do próprio core que não seja atendida pelo ecossistema do Nano-Spring.

---

## 2. Arquitetura em Camadas Recomendada

Para manter a separação de responsabilidades e viabilizar testes unitários e manutenibilidade, utilize uma arquitetura em camadas no pacote base escaneado:

```text
com.example.myapp/
│
├── controller/          # Camada Web / HTTP (@RestController)
│   ├── ProductController.java
│   └── SystemController.java
│
├── service/             # Camada de Regras de Negócio e Serviços Android
│   ├── AppBackendService.java   # Foreground Service (extende NanoSpringService)
│   ├── ProductService.java      # (@Service) Regras de negócio
│   └── impl/                    # (Opcional) Implementações de interfaces de serviço
│
├── repository/          # Camada de Persistência (@Repository)
│   ├── ProductRepository.java
│   └── DatabaseOpenHelper.java
│
├── dto/                 # Data Transfer Objects (Requisição / Resposta)
│   ├── request/
│   │   └── CreateProductRequest.java
│   └── response/
│       └── ProductResponse.java
│
├── model/ ou entity/    # Entidades de Domínio / Banco de Dados
│   └── Product.java
│
├── exception/           # Tratamento de Erros e Exceções Customizadas
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
│
├── config/              # Configurações de Beans, CORS, Interceptors
│   └── WebConfig.java
│
└── ui/                  # Interface Nativa Android (Separada da Web)
    ├── MainActivity.java        # Ciclo de Vida da UI / Painel de Controle do Servidor
    ├── viewmodel/               # ViewModels para telas nativas
    └── adapter/                 # Adapters de RecyclerView, etc.
```

---

## 3. Padrão de Separação de Interfaces: Web vs Android Nativo

> [!IMPORTANT]
> **A interface Web (HTTP / REST / Dashboard) e a interface nativa do Android (Activity / Compose / XML) devem ser completamente desacopladas.**

A aplicação atua simultaneamente como um servidor e como um aplicativo móvel local. Para manter a estabilidade e a modularidade:

### 3.1 Responsabilidades de Cada Interface

| Aspecto | Interface Android Nativa (`ui/`, Activities, Compose) | Interface Web / HTTP (`controller/`, HTML/Assets, REST) |
| :--- | :--- | :--- |
| **Público / Consumidor** | O operador segurando o dispositivo físico. | Clientes externos na rede local, navegadores, totens, PDVs ou dashboards web. |
| **Papel Principal** | Painel de controle, status do servidor (IP, porta, logs, bateria), inicialização e permissões. | Expor regras de negócio, dados, APIs REST, Server-Sent Events (SSE) ou servir assets estáticos/SPAs (`assets/static/`). |
| **Dependência de Ciclo de Vida** | Vinculada ao ciclo de vida de UI do Android (`Activity`, `Fragment`, recomposição de telas). Pode ser destruída em rotação ou quando o app vai para background. | Persistente, executada dentro do `NanoSpringService` (Foreground Service), operando independentemente da UI do Android estar visível ou destruída. |

### 3.2 Regras de Ouro de Isolamento

1. **A camada Web não depende de Views nativas:**
   - Controllers (`@RestController`) e Services (`@Service`) **nunca** devem receber referências a `Activity`, `View`, `Context` com leak de interface ou elementos de UI nativa.
   - O único `Context` compartilhado com o servidor é o `ApplicationContext` (disponível via `NanoSpringService`).

2. **A UI Android é apenas cliente/observadora do Backend:**
   - A `MainActivity` ou os componentes nativos de tela devem interagir com o backend gerenciando o serviço (`startForegroundService` / `stopService`) ou consumindo o backend via cliente HTTP local (`http://localhost:8080`) / repositórios e serviços compartilhados em background.
   - Mudanças de estado para a UI devem ser comunicadas de forma desacoplada (via `LiveData`, `StateFlow`, `BroadcastReceiver` ou `EventBus`), nunca chamando métodos diretos de UI a partir do fluxo de execução do servidor HTTP.

3. **Interface Web com HTMX e Templates HTML (Sem HTML Inline):**
   - **É terminantemente desencorajado e fora de padrão concatenar ou retornar HTML inline (strings com tags HTML hardcoded) dentro dos controllers.**
   - A renderização dinâmica Server-Side deve adotar **HTMX** em conjunto com a engine de templates Mustache do Nano-Spring via `ModelAndView` (`com.github.matheuscruzsouza.nanospring.ui.ModelAndView`).
   - Todos os arquivos de template HTML devem residir exclusivamente em `app/src/main/assets/templates/` (ex.: `painel.html`, `partials/produto-linha.html`).
   - O HTMX permite interações reativas e trocas parciais de tela (swaps) sob demanda sem a complexidade ou consumo de memória de frameworks SPA pesados no dispositivo Android.

4. **Arquivos Estáticos da Web:**
   - Assets estáticos como a biblioteca `htmx.min.js`, scripts JS, CSS e imagens devem residir em `app/src/main/assets/static/` e são servidos automaticamente sob a rota `/static/*` com headers de cache apropriados.

#### Exemplo de Padrão Web: HTMX + ModelAndView

**Template (`assets/templates/produtos.html`):**
```html
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <title>{{titulo}}</title>
    <!-- HTMX servido dos assets locais -->
    <script src="/static/js/htmx.min.js"></script>
</head>
<body>
    <h1>{{titulo}}</h1>

    <!-- Busca reativa via HTMX enviando requisição para o controller -->
    <input type="text" name="termo" placeholder="Buscar produto..."
           hx-get="/web/produtos/tabela"
           hx-trigger="keyup changed delay:300ms"
           hx-target="#lista-produtos">

    <div id="lista-produtos">
        <!-- Renderiza a lista inicial via template -->
        <ul>
            {{#produtos}}
            <li>{{nome}} - R$ {{preco}}</li>
            {{/produtos}}
        </ul>
    </div>
</body>
</html>
```

**Controller Web (`WebProductController.java`):**
```java
package com.example.myapp.controller;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RequestParam;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.ui.ModelAndView;
import com.example.myapp.service.ProductService;

@RestController("/web/produtos")
public class WebProductController {

    private final ProductService productService;

    @Autowired
    public WebProductController(ProductService productService) {
        this.productService = productService;
    }

    // Retorna a página completa renderizada a partir de assets/templates/produtos.html
    @GetMethod("")
    public ModelAndView exibirPagina() {
        return new ModelAndView("produtos")
                .addObject("titulo", "Catálogo de Produtos")
                .addObject("produtos", productService.findAll(null));
    }

    // Retorna fragmento parcial para swap do HTMX a partir de assets/templates/partials/lista.html
    @GetMethod("/tabela")
    public ModelAndView buscarTabela(@RequestParam(value = "termo", required = false) String termo) {
        return new ModelAndView("partials/lista")
                .addObject("produtos", productService.findAll(termo));
    }
}
```

> [!WARNING]
> **Anti-Pattern (Evite):**
> ```java
> // NUNCA faça isso: HTML inline no controller polui o código e quebra a separação de responsabilidades
> @GetMethod("/pagina")
> public String paginaRuim() {
>     return "<html><body><h1>" + titulo + "</h1></body></html>";
> }
> ```

---

## 4. Padrões por Camada

### 4.1 Camada Web: Controllers (`@RestController`)

**Responsabilidades:**
- Declarar endpoints e mapear rotas HTTP.
- Validar entrada de dados com `@Valid`.
- Delegar o processamento estritamente para a camada de serviço.
- Retornar respostas tipadas com `ResponseEntity<T>`.

**Boas Práticas:**
- Prefira injetar dependências via construtor com `@Autowired`.
- Evite regras de negócio, cálculos ou acesso direto ao SQLite dentro de métodos de controller.
- Use a anotação `@RestController("/api/recurso")` para definir o prefixo da rota base.
- Em parâmetros dinâmicos na URL, utilize a sintaxe de dois pontos (ex.: `/:id`) e resgate com `@PathVariable("id")`.

```java
package com.example.myapp.controller;

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

import com.example.myapp.dto.request.CreateProductRequest;
import com.example.myapp.dto.response.ProductResponse;
import com.example.myapp.service.ProductService;

import java.util.List;

@RestController("/api/products")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMethod("")
    public ResponseEntity<List<ProductResponse>> listAll(
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(productService.findAll(category));
    }

    @GetMethod("/:id")
    public ResponseEntity<ProductResponse> getById(@PathVariable("id") String id) {
        ProductResponse product = productService.findById(id);
        return ResponseEntity.ok(product);
    }

    @PostMethod("")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

---

### 4.2 Camada de Domínio & DTOs (Validação)

**Responsabilidades:**
- Desacoplar os contratos da API externa das tabelas internas do banco de dados.
- Proteger a aplicação contra payloads inválidos, nulos ou malformados.

**Boas Práticas:**
- Sempre crie DTOs dedicados para entrada de dados (`Request`) e saída (`Response`).
- Tenha construtor sem argumentos (padrão JavaBean) e métodos getters/setters para serialização JSON via Gson.
- Use anotações de validação do Nano-Spring: `@NotNull`, `@Size`, `@NotEmpty`, etc.

```java
package com.example.myapp.dto.request;

import com.github.matheuscruzsouza.nanospring.annotation.validation.NotNull;
import com.github.matheuscruzsouza.nanospring.annotation.validation.Size;

public class CreateProductRequest {

    @NotNull(message = "O nome é obrigatório")
    @Size(min = 2, max = 80, message = "O nome deve conter entre 2 e 80 caracteres")
    private String name;

    @NotNull(message = "O preço é obrigatório")
    private Double price;

    public CreateProductRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
```

---

### 4.3 Camada de Negócio: Serviços (`@Service`)

**Responsabilidades:**
- Orquestrar regras de negócio, validações de fluxo e chamadas a repositórios.
- Isolar a lógica da camada de transporte HTTP (um service nunca deve lidar com `IHTTPSession` ou `ResponseEntity`).
- Lançar exceções de negócio em casos anômalos.

**Boas Práticas:**
- Mantenha classes de serviço anotadas com `@Service`.
- Evite armazenar estado mutável compartilhado nas instâncias de `@Service` (são singletons no container IoC).
- Use injeção via construtor para facilitar testes com mocks/stubs.

```java
package com.example.myapp.service;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.Service;

import com.example.myapp.dto.request.CreateProductRequest;
import com.example.myapp.dto.response.ProductResponse;
import com.example.myapp.exception.ResourceNotFoundException;
import com.example.myapp.model.Product;
import com.example.myapp.repository.ProductRepository;

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
            throw new ResourceNotFoundException("Produto não encontrado: " + id);
        }
        return toResponse(entity);
    }

    public List<ProductResponse> findAll(String category) {
        return productRepository.findAll(category)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse create(CreateProductRequest request) {
        Product entity = new Product(
                UUID.randomUUID().toString(),
                request.getName(),
                request.getPrice()
        );
        productRepository.save(entity);
        return toResponse(entity);
    }

    private ProductResponse toResponse(Product entity) {
        return new ProductResponse(entity.getId(), entity.getName(), entity.getPrice());
    }
}
```

---

### 4.4 Camada de Persistência: Repositórios (`@Repository`)

**Responsabilidades:**
- Encapsular comandos SQL, mapeamento de `Cursor` para objetos de modelo e transações SQLite.
- Gerenciar recursos de forma segura (fechamento de cursores e statements).

**Boas Práticas:**
- Marque repositórios com `@Repository`.
- Sempre utilize parâmetros vinculados (`selectionArgs` no Android) para prevenir SQL Injection.
- Feche `Cursor` dentro de blocos `try-with-resources`.

```java
package com.example.myapp.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.Repository;
import com.example.myapp.model.Product;

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
        String sql = "SELECT id, name, price FROM products WHERE id = ?";
        try (Cursor cursor = database.rawQuery(sql, new String[]{id})) {
            if (cursor.moveToFirst()) {
                return mapCursor(cursor);
            }
        }
        return null;
    }

    public List<Product> findAll(String category) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT id, name, price FROM products";
        try (Cursor cursor = database.rawQuery(sql, null)) {
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
        values.put("price", product.getPrice());
        database.insertWithOnConflict("products", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private Product mapCursor(Cursor cursor) {
        return new Product(
                cursor.getString(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("price"))
        );
    }
}
```

---

### 4.5 Camada de Segurança: Autenticação via Interceptor (`@Interceptor`)

**Responsabilidades:**
- Validar tokens de autorização (`Authorization: Bearer <token>`) e permissões antes que a requisição alcance os controllers.
- Liberar rotas públicas e recursos estáticos/templates sem acoplar validações manuais de token dentro de cada método de controller.

**Boas Práticas:**
- Implemente `HandlerInterceptor` anotado com `@Interceptor` e `@Order(1)`.
- Use verificação precoce de paths: libere rotas públicas (como `/api/public/**`, `/static/**`, assets) e bloqueie requisições não autorizadas retornando `false` (que responde automaticamente com `401 Unauthorized`).

```java
package com.example.myapp.security;

import com.github.matheuscruzsouza.nanospring.annotation.Interceptor;
import com.github.matheuscruzsouza.nanospring.annotation.Order;
import com.github.matheuscruzsouza.nanospring.handler.HandlerInterceptor;
import fi.iki.elonen.NanoHTTPD;

@Interceptor
@Order(1)
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(NanoHTTPD.IHTTPSession session, String path) {
        // Libera rotas públicas, estáticos e interfaces web abertas
        if (path.startsWith("/api/public") || path.startsWith("/static") || path.startsWith("/web")) {
            return true;
        }

        // Validação de token em rotas protegidas
        String authHeader = session.getHeaders().get("authorization");
        if (authHeader == null || !isValidToken(authHeader)) {
            return false; // Cancela a requisição e retorna 401 Unauthorized
        }

        return true;
    }

    private boolean isValidToken(String authHeader) {
        return authHeader.startsWith("Bearer ") && authHeader.length() > 15;
    }
}
```

---

### 4.6 Camada de Tratamento Global de Exceções (`@ControllerAdvice`)

**Responsabilidades:**
- Interceptar exceções de negócio, falhas de validação e erros inesperados em um ponto único.
- Garantir que a API **nunca** retorne stack traces puros do Java ou erros genéricos 500 sem corpo JSON explicativo.

**Boas Práticas:**
- Crie uma classe anotada com `@ControllerAdvice`.
- Mapeie exceções específicas com métodos anotados por `@ExceptionHandler(ClasseDaException.class)`.
- Retorne contratos padronizados (`ResponseEntity<Map<String, Object>>` ou um DTO de erro com `status`, `error`, `message`, `timestamp`).

```java
package com.example.myapp.exception;

import com.github.matheuscruzsouza.nanospring.annotation.ControllerAdvice;
import com.github.matheuscruzsouza.nanospring.annotation.ExceptionHandler;
import com.github.matheuscruzsouza.nanospring.http.HttpStatus;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 404);
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        body.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", "Ocorreu um erro inesperado no servidor local");
        body.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
```

---

## 5. Padrões de Execução Android e Ciclo de Vida

O Nano-Spring roda dentro do processo Android. Para garantir que o servidor não seja encerrado pelo sistema operacional (Low Memory Killer / Doze Mode):

### 5.1 Utilizar `NanoSpringService` (Foreground Service)
- **Sempre execute o servidor como Foreground Service** associado a uma notificação persistente.
- Herde de `NanoSpringService` para obter controle integrado de `WakeLock` e inicialização.

```java
package com.example.myapp.service;

import com.github.matheuscruzsouza.nanospring.service.NanoSpringService;

public class AppBackendService extends NanoSpringService {

    @Override
    protected String getBasePackage() {
        return "com.example.myapp";
    }

    @Override
    protected int getPort() {
        return 8080;
    }
}
```

### 5.2 Permissões e Declaração no `AndroidManifest.xml`
Declare os tipos corretos de serviço para Android 14+ (`API 34+`):

```xml
<!-- Permissões de Rede -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />

<!-- Foreground Service & Notificações -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<application ...>
    <service
        android:name=".service.AppBackendService"
        android:enabled="true"
        android:exported="false"
        android:foregroundServiceType="dataSync" />
</application>
```

---

## 6. Padrão de Descoberta de Rede Local: mDNS / Zeroconf

Como o Nano-Spring opera em dispositivos móveis conectados via Wi-Fi local onde os endereços IP podem mudar via DHCP, **o anúncio mDNS é o padrão para descoberta e resolução de nomes**.

### 6.1 Dependência no `app/build.gradle`
Adicione o módulo `nano-spring-discovery`:

```kotlin
dependencies {
    implementation("com.github.matheuscruzsouza:nano-spring-discovery:2.4.1")
}
```

### 6.2 Configuração em `assets/application.properties`
Ative a resolução de host para que qualquer navegador ou cliente na rede possa acessar o servidor via `http://<nome-do-host>.local:<porta>`:

```properties
# Habilita o anúncio de rede mDNS
nano.nsd.enabled=true

# Nome do host anunciado na rede (acessível como http://meu-dispositivo.local:8080)
nano.nsd.name=meu-dispositivo

# Tipo do serviço DNS-SD (padrão: _http._tcp.)
nano.nsd.type=_http._tcp.

# Habilita o responder direto para navegadores web (padrão: true)
nano.nsd.host-resolution=true
```

> [!NOTE]
> O módulo inclui o `MdnsHostResponder`, que responde a consultas mDNS diretas enviadas por navegadores (Chrome, Safari, Firefox) na porta UDP 5353 com o IP ativo do dispositivo. Certifique-se de declarar a permissão `android.permission.CHANGE_WIFI_MULTICAST_STATE` no `AndroidManifest.xml`.

---

## 7. Diretrizes de Concorrência e Hardware em Dispositivos Móveis

1. **Previsibilidade de Memória:**
   - Mantenha o consumo controlado (entre 15 MB e 40 MB de heap).
   - Evite carregar coleções inteiras em memória. Use paginação e limite as consultas (`LIMIT / OFFSET`).
   - Não crie novas `Thread` soltas a cada requisição; utilize executores com filas limitadas.

2. **Streams & Arquivos Grandes:**
   - Para envio de arquivos, imagens e vídeos, utilize streams (`FileResourceRegion`, `InputStream`) em vez de carregar arrays de bytes (`byte[]`) completos na memória RAM.

3. **Eventos em Tempo Real (SSE):**
   - Utilize `SseEmitter` com controle adequado de expiração (timeouts) e limpeza de emitters desconectados em blocos `onCompletion` / `onError`.

4. **Consciência de Bateria e Térmica:**
   - Observe o status de bateria através das APIs nativas (`BatteryService` / `BatteryHelper`).
   - Reduza a frequência de tarefas de background em caso de bateria fraca ou superaquecimento térmico.

---

---

## 8. Diretrizes Opcionais: O que Perguntar ao Desenvolvedor Antes de Implementar

> [!NOTE]
> Para evitar complexidade acidental e sobrecarga de dependências (respeitando a filosofia *Simplicity First* e *Zero Speculative Code*), certas funcionalidades permanecem **estritamente opcionais**. 
> **Assistentes de IA e desenvolvedores devem sempre perguntar e alinhar a necessidade antes de incluí-las no projeto:**

### 8.1 Módulos e Recursos Opcionais (Matriz de Decisão)

| Recurso / Módulo | Quando Perguntar ao Desenvolvedor | Padrão Recomendado se Aprovado |
| :--- | :--- | :--- |
| **Persistência SQLite & Migrações** (`nano-spring-data`) | *"A aplicação precisará de persistência em banco de dados local ou apenas dados em memória/comunicação em rede?"* | Se sim: usar `@Repository`, scripts SQL sequenciais (`V1__init.sql`) em `assets/db/migrations/` via `MigrationManager`. |
| **Proteção contra DoS / Rate Limiting** | *"A API será exposta em rede Wi-Fi aberta/compartilhada exigindo limite de requisições por IP, ou será em rede controlada/ponto a ponto?"* | Se sim: configurar `TokenBucketRateLimiter` no `application.properties` para prevenir esgotamento de CPU/bateria. |
| **CORS e Políticas de Origem** | *"Haverá clientes acessando a API a partir de navegadores web externos (SPAs/cross-origin)?"* | Se sim: declarar as origens permitidas explicitamente no `application.properties` ou configuração de CORS. |
| **Documentação Interativa (OpenAPI / Swagger)** | *"Deseja incluir a interface interativa do Swagger (`/swagger-ui/`) para testes locais e documentação pública das rotas?"* | Se sim: adicionar as anotações `@Tag`, `@Operation` e `@ApiResponse` nos controllers. |

---

## 9. Checklist para Novos Projetos

- [ ] Uso exclusivo dos recursos e abstrações do **Nano-Spring** antes de tentar implementar ou importar classes diretas do **NanoHTTPD**.
- [ ] Separação explícita entre a interface Web (`controller/`, assets) e a interface nativa Android (`ui/`).
- [ ] Uso padronizado de **HTMX** e templates Mustache em `assets/templates/` via `ModelAndView`, **sem HTML inline** nos controllers.
- [ ] Implementação de **Autenticação centralizada** via `@Interceptor` (`HandlerInterceptor`) com `@Order(1)`.
- [ ] Implementação de **Tratamento Centralizado de Exceções** via `@ControllerAdvice` retornando respostas de erro JSON padronizadas.
- [ ] Registro e anúncio **mDNS / Zeroconf** (`nano-spring-discovery`) com resolução de host habilitada e permissão `CHANGE_WIFI_MULTICAST_STATE`.
- [ ] Confirmação prévia com o usuário/desenvolvedor sobre recursos opcionais (`nano-spring-data`, Swagger, CORS, Rate Limiter).
- [ ] Nenhum vazamento de instâncias de `Activity`, `View` ou referências diretas de UI para `@RestController` ou `@Service`.
- [ ] Pacote base devidamente configurado e sem referências a `org.springframework.*`.
- [ ] Dependências `nano-spring-core`, `nano-spring-web` e `nano-spring-discovery` adicionadas ao `build.gradle`.
- [ ] Permissões de rede, multicast e Foreground Service configuradas no `AndroidManifest.xml`.
- [ ] Subclasse de `NanoSpringService` criada e registrada no manifest.
- [ ] DTOs com validações (`@Valid`, `@NotNull`, `@Size`) nos endpoints que recebem `@RequestBody`.
- [ ] Camada de serviço separada da camada de controle e persistência.
