# Nano-Spring 🌱

**Nano-Spring** é um micro-framework web ultra-leve construído exclusivamente para **Android**. Ele traz a elegância, produtividade e o padrão arquitetural do **Spring Boot** para dentro do ambiente móvel, utilizando o NanoHTTPD por baixo dos panos.

Com o Nano-Spring, você transforma qualquer celular ou tablet Android em um poderoso servidor web (Frontend e Backend), escrevendo código limpo, sem boilerplate e focado em anotações.

---

## 🌟 Principais Funcionalidades

- 📜 **Diagnósticos & Rotação de Logs (v1.9.0):** `RotatingFileLogger` com teto de disco e endpoint remoto `/actuator/logfile`.
- 📑 **Swagger & OpenAPI 3.0 Embutido (v1.10.0):** Interface interativa embutida (`/swagger-ui`) e OpenAPI 3.0.1 (`/v3/api-docs`) ultra-leve (~15 KB) e 100% offline.
- ⚙️ **Perfis de Ambiente Multi-Camada (v1.9.0):** Suporte a `application-{profile}.properties` com ativação dinâmica.
- ⚡ **Controladores Assíncronos (v1.9.0):** Retornos com `CompletableFuture<T>`, timeout automático (`408`) e proteção de threads de hardware (POS/TEF).
- ⏱️ **Timeout de Leitura Configurável (v1.9.0):** `nano.server.read-timeout` para redes instáveis ou dispositivos embarcados.
- 🔒 **HTTPS / TLS Nativo:** Tráfego criptografado com Keystores `.p12` / `.bks` sem alterar código.
- 🛡️ **Rate Limiting Anti-DoS:** Algoritmo *Token Bucket* thread-safe em memória por IP (global e `@RateLimit`).
- 📋 **Bean Validation Declarativo:** Validação automática com `@Valid`, `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Email`, `@Pattern`.
- 🏢 **Módulo Enterprise & Resiliência:** Pool de threads anti-OOM (`ThreadPoolAsyncRunner`), SQLite WAL mode concorrente e reconexão de rede (`NetworkWatcher`).
- 📊 **Actuator & Observabilidade:** Endpoints `/actuator/health`, `/actuator/info` e `/actuator/logfile` expondo telemetria e logs remotos.
- ⚡ **Classe Base `NanoSpringService`:** Foreground Service Android pré-configurado com canais de notificação e Wake/Multicast locks.
- 🚀 **Roteamento RESTful:** `@RestController`, `@GetMethod`, `@PostMethod`, `@PutMethod`, `@DeleteMethod`, `@RequestHeader`.
- 💉 **Injeção de Dependências (DI):** Gerenciamento automático de ciclo de vida com `@Service`, `@Repository` e `@Autowired`.
- 🗄️ **Banco de Dados SQLite & Migrações:** Suporte nativo ao SQLite com versionamento de esquemas por arquivos SQL (estilo Flyway).
- 🔍 **Descoberta de Serviço (mDNS / DNS-SD):** Anúncio automático do servidor na rede local via Zeroconf / Bonjour com NsdManager nativo.
- ⚙️ **Configurações Externalizadas:** Suporte nativo a `application.properties` e injeção via `@Value`.
- 📡 **Server-Sent Events (SSE):** Streaming de eventos unidirecionais em tempo real para navegadores com `SseEmitter`.
- 📁 **Upload de Arquivos Nativo:** Receba imagens e documentos facilmente via *multipart/form-data* usando `@UploadedFile`.
- 🛡️ **Middlewares (Interceptadores):** Intercepte requisições nativamente para validação de JWTs, logs e segurança.
- 🌐 **CORS Nativo Declarativo:** Suporte a Cross-Origin Resource Sharing com resolução automática de pre-flight (`OPTIONS`).
- 🚨 **Tratamento Global de Exceções:** Evite crashes de rotas usando `@ControllerAdvice` e `@ExceptionHandler`.
- 🎨 **Motor de Templates HTML:** Renderização Server-Side nativa utilizando `JMustache` e `ModelAndView`.
- ⚡ **Servidor de Arquivos Estáticos:** Entregue CSS, JS e Imagens da pasta `assets` sem esforço.

---

## 📱 Requisitos do Sistema & Hardware

O **Nano-Spring** foi projetado para ser ultra-leve e rodar perfeitamente até mesmo em smartphones de entrada ou aparelhos antigos reaproveitados como servidores domésticos.

### Requisitos de Software
- **Versão Mínima do Android:** Android 7.1.1 (Nougat)
- **API Mínima (minSdk):** API 25
- **API Alvo / Compilação:** API 34+ (compilado com API 36)
- **Java Compatibility:** Java 11+

### Requisitos Mínimos de Hardware
- **Memória RAM:** 1 GB (o servidor consome entre **15 MB e 40 MB de RAM** em execução normal).
- **Processador (CPU):** Quad-Core 1.3 GHz (qualquer chip ARMv7, ARM64 ou x86_64).
- **Armazenamento:** Menos de **5 MB** de espaço para a biblioteca.
- **Rede:** Conexão Wi-Fi, Ethernet ou dados móveis para expor a porta local (ex: `8080` ou `9090`).

### Requisitos Recomendados (Cargas Maiores / Uploads Pesados)
- **Memória RAM:** 2 GB ou mais.
- **Processador (CPU):** Octa-Core 2.0 GHz ou superior (permite atender dezenas de conexões concorrentes sem perda de throughput).
- **Dica de Bateria:** Manter o aparelho conectado à fonte de energia e desativar a "Otimização de Bateria" para o app nas configurações do Android.

---

## 📦 Como Instalar

### Opção 1: Como Módulo Local (Dentro do mesmo projeto)
No arquivo `build.gradle` do seu aplicativo principal (`app`):
```gradle
dependencies {
    implementation project(':nano-spring')
}
```

### Opção 2: Via GitHub Packages (Como dependência remota)

> ⚠️ **Importante sobre o GitHub Packages:** O GitHub exige autenticação com token (Personal Access Token) para baixar pacotes, mesmo que o repositório seja público.

#### 1. Configurar suas credenciais do GitHub
No arquivo global `~/.gradle/gradle.properties` (ou no `gradle.properties` do seu projeto):
```properties
gpr.user=SEU_USUARIO_GITHUB
gpr.key=SEU_GITHUB_PERSONAL_ACCESS_TOKEN
```
*(O token precisa apenas do escopo `read:packages`).*

#### 2. Declarar o repositório no `settings.gradle`:
```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/matheuscruzsouza/nano-spring")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR") ?: ""
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}
```

#### 3. Adicionar a dependência no `build.gradle` do seu app:
```gradle
dependencies {
    implementation 'com.github.matheuscruzsouza:nano-spring:1.10.0'
}
```

O Nano-Spring já encapsula as seguintes dependências:
- `org.nanohttpd:nanohttpd:2.3.1`
- `com.google.code.gson:gson:2.8.9`
- `com.samskivert:jmustache:1.15`

---

## 🚀 Guia de Uso Completo e Exemplos

### 1. Inicializando o Servidor (Via `NanoSpringService` ou Manual)

#### Opção A: Herdar de `NanoSpringService` (Recomendado para Produção / Foreground)
A classe base cuida de `NotificationChannel`, notificação permanente, `WakeLock`, `MulticastLock` e parada limpa:

```java
package com.seupacote.app;

import com.github.matheuscruzsouza.nanospring.service.NanoSpringService;

public class MyBackendService extends NanoSpringService {
    @Override
    protected String getBasePackage() {
        return "com.seupacote.app";
    }

    @Override
    protected int getPort() {
        return 8080;
    }
}
```

#### Opção B: Instanciação Direta
```java
Server server = new Server(context, 8080, "com.seupacote.app");
```

### 2. Configurações (`application.properties`) e `@Value`
Crie um arquivo `application.properties` na sua pasta `assets/`.
```properties
server.port=9090
api.key=MINHA_CHAVE_SECRETA
```

Injete os valores diretamente nas suas classes:
```java
@RestController("/api/config")
public class ConfigController {

    // Se não achar a chave, usa o valor padrão "Chave_Padrao"
    @Value("${api.key:Chave_Padrao}") 
    private String apiKey;

    @GetMethod("")
    public String getKey() {
        return "Sua chave é: " + apiKey;
    }
}
```

### 3. Criando APIs REST (JSON)
A extração de variáveis da URL, parâmetros de query e conversão de JSON no Body é feita de forma automática via Reflection.

```java
@RestController("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Acessível via GET /api/users/123 com captura de Header
    @GetMethod("/:id")
    public User getUser(
            @PathVariable("id") String id,
            @RequestHeader("Authorization") String token) {
        return userService.findById(Integer.parseInt(id));
    }

    // Acessível via POST /api/users com JSON no Body e status 201 Created
    @PostMethod("")
    public ResponseEntity<User> createUser(@RequestBody User newUser) {
        User salvo = userService.save(newUser);
        return ResponseEntity.created("/api/users/" + salvo.getId()).body(salvo);
    }
}
```

#### Respostas com `ResponseEntity` (Padrão Spring Boot):
Você pode controlar status HTTP e cabeçalhos fluentes com a classe `ResponseEntity`:
```java
// 200 OK com corpo
return ResponseEntity.ok(user);

// 200 OK com builder fluente
return ResponseEntity.ok().header("X-Custom", "123").body(user);

// 201 Created com header Location
return ResponseEntity.created("/api/users/10").body(user);

// 204 No Content
return ResponseEntity.noContent().build();

// 404 Not Found
return ResponseEntity.notFound().build();

// Resolução de Optional (200 OK ou 404 Not Found)
return ResponseEntity.of(userService.findOptionalById(id));
```

### 4. Upload de Arquivos (`multipart/form-data`)
Receber arquivos de um formulário Web ou de um App cliente é trivial. Basta usar `@UploadedFile`.

```java
@RestController("/api/uploads")
public class UploadController {

    @PostMethod("")
    public String handleFileUpload(@UploadedFile("fotoPerfil") java.io.File arquivo) {
        if (arquivo == null) return "Nenhum arquivo recebido!";
        
        // O arquivo já foi baixado para o Cache do Android. 
        // Agora você pode movê-lo para um armazenamento permanente.
        long tamanhoEmBytes = arquivo.length();
        return "Upload de " + tamanhoEmBytes + " bytes concluído!";
    }
}
```

### 5. Download e Streaming de Arquivos (`java.io.File`)
Seu endpoint pode retornar diretamente uma instância de `java.io.File`. O Nano-Spring ativa transmissão em blocos (*Chunked Transfer*) e adiciona o header `Content-Disposition` para download direto:

```java
@RestController("/api/files")
public class DownloadController {

    @GetMethod(value = "/download/:id", mimeType = "application/octet-stream")
    public Object downloadFile(@PathVariable("id") String id) {
        File file = fileService.getFileOnDisk(id);
        if (file.exists()) {
            return file; // O Nano-Spring faz streaming automático via chunked response
        }
        return "Arquivo não encontrado";
    }
}
```

### 6. Server-Sent Events (SSE) com `SseEmitter`
Transmita notificações, métricas ou atualizações em tempo real diretamente para o navegador (compatível nativamente com o padrão JavaScript `EventSource` e extensões do HTMX):

```java
import com.github.matheuscruzsouza.nanospring.sse.SseEmitter;

@RestController("/api/stream")
public class NotificationController {

    @GetMethod(value = "/alerts", mimeType = "text/event-stream")
    public SseEmitter streamAlerts() {
        SseEmitter emitter = new SseEmitter();

        new Thread(() -> {
            try {
                emitter.send("Conexão estabelecida com sucesso!");
                
                for (int i = 1; i <= 5; i++) {
                    Thread.sleep(1000);
                    // Pode enviar objetos (convertidos para JSON) ou eventos nomeados:
                    emitter.send("alerta", Map.of("step", i, "status", "Processando"));
                }

                emitter.complete(); // Encerra o fluxo
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}
```

No frontend (JavaScript):
```javascript
const eventSource = new EventSource('/api/stream/alerts');
eventSource.addEventListener('alerta', (event) => {
    const data = JSON.parse(event.data);
    console.log("Recebido:", data);
});
```

### 7. Interceptadores (Segurança e Logs)
Crie classes que implementam `HandlerInterceptor` para barrar ou auditar rotas antes que elas cheguem no Controller.

```java
@Interceptor
@Order(1) // Ordem de execução (1 é o primeiro)
public class SecurityInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(NanoHTTPD.IHTTPSession session, String path) {
        // Bloqueia rotas /admin se não enviar o cabeçalho Authorization
        if (path.startsWith("/admin")) {
            String token = session.getHeaders().get("authorization");
            if (token == null || !token.equals("Bearer SEGREDO")) {
                return false; // Bloqueia a requisição retornando 401 Unauthorized
            }
        }
        return true; // Deixa passar para o Controller
    }
}
```

### 8. Tratamento de Exceções Globais
Cansado de blocos Try-Catch espalhados? Crie um Advice para responder adequadamente caso seu código dispare exceções (ex: formato de ID errado ou registro não encontrado).

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NumberFormatException.class)
    public ErrorResponse handleFormatError(NumberFormatException ex) {
        return new ErrorResponse("BAD_REQUEST", "Você enviou letras no lugar de números!");
    }

    @ExceptionHandler(IndexOutOfBoundsException.class)
    public ErrorResponse handleNotFoundError(IndexOutOfBoundsException ex) {
        return new ErrorResponse("NOT_FOUND", "O recurso solicitado não existe.");
    }
}
```

### 9. Retornando Páginas HTML e HTMX
Para renderizar templates HTML (que devem ser colocados em `assets/templates/`), retorne um `ModelAndView`.

```java
@RestController("/web")
public class WebController {

    @GetMethod("/profile/:id")
    public ModelAndView renderProfile(@PathVariable("id") String id) {
        User user = userService.findById(id);
        // O motor JMustache injetará as variáveis {{username}} no arquivo user_profile.html
        return new ModelAndView("user_profile")
                .addObject("username", user.getName());
    }
}
```
**Para HTMX:** Você também pode retornar um `String` simples definindo o `mimeType = "text/html"`, ideal para devolver pedaços/fragmentos de tela sem recarregar a página.

### 10. Arquivos Estáticos (CSS, JS, Imagens)
Coloque qualquer arquivo estático dentro de `app/src/main/assets/static/`.
O Nano-Spring hospeda essa pasta automaticamente! No seu HTML, basta linkar:
```html
<link rel="stylesheet" href="/static/css/style.css">
<script src="/static/js/main.js"></script>
```

### 11. Banco de Dados SQLite & Migrações (Estilo Flyway)
O Nano-Spring possui suporte nativo ao **SQLite do Android** com controle de evolução de esquemas por meio de arquivos `.sql` versionados.

#### 1. Criando as migrações
Coloque seus scripts SQL na pasta `assets/db/migration/`:
- `assets/db/migration/V1__create_users_table.sql`
- `assets/db/migration/V2__add_status_column.sql`

Exemplo (`V1__create_users_table.sql`):
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE
);

INSERT INTO users (nome, email) VALUES ('Administrador', 'admin@local.com');
```

O Nano-Spring cria automaticamente a tabela de controle `nano_schema_history` no SQLite, rastreia as versões aplicadas e executa cada script pendente dentro de uma transação.

#### 2. Configurações opcionais (`application.properties`):
```properties
nano.datasource.name=meu_banco.db
nano.datasource.migration.location=db/migration
nano.datasource.migration.enabled=true
```

#### 3. Usando com `@Repository` e `@Autowired`:
O `SQLiteDatabase` é automaticamente registrado como singleton no container de injeção:

```java
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.Repository;

@Repository
public class UserRepository {

    @Autowired
    private SQLiteDatabase db;

    public List<String> listUsers() {
        List<String> list = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("SELECT nome FROM users", null)) {
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0));
            }
        }
        return list;
    }
}
```

### 12. Descoberta de Serviço e Acesso Direto via Navegador (`.local`)
O Nano-Spring possui suporte integrado ao anúncio de rede mDNS / DNS-SD (Zeroconf / Bonjour) e **resolução direta de Host (registros A)**.

Isso significa que você pode acessar seu servidor digitando diretamente no navegador (Chrome, Firefox, Safari, Edge):
```text
http://meu-servidor-android.local:8080/
```
Sem precisar saber o IP do celular, sem configurar roteador e sem instalar nada!

#### 1. Configuração (`application.properties`):
```properties
# Habilita o anúncio e descoberta mDNS
nano.nsd.enabled=true

# Nome do host e serviço (acessível como http://meu-servidor-android.local:porta)
nano.nsd.name=meu-servidor-android

# Tipo do serviço DNS-SD (padrão: _http._tcp.)
nano.nsd.type=_http._tcp.

# Habilita o responder mDNS para resolução de Host A/AAAA no navegador (padrão: true)
nano.nsd.host-resolution=true
```

#### 2. Ativação programática ou injeção:
Você também pode ativar, desativar ou injetar o gerenciador em tempo de execução:
```java
// Ativar programaticamente no Server com resolução de host:
server.enableNsd("meu-servidor", true);

// Ou desativar:
server.disableNsd();
```
Quando o servidor é parado com `server.stop()`, o serviço e a resolução mDNS são automaticamente desregistrados da rede.

### 13. Suporte Nativo a CORS (Cross-Origin Resource Sharing)
O Nano-Spring possui suporte nativo e declarativo a **CORS**, habilitado por padrão para requerer o mínimo de configuração do desenvolvedor. Ele resolve automaticamente requisições pre-flight (`OPTIONS`) com resposta `200 OK` vazia e injeta os cabeçalhos apropriados em todas as rotas (incluindo respostas de erro e arquivos estáticos).

#### 1. Configurações (`application.properties`):
```properties
# Habilita ou desabilita o filtro global de CORS (padrão: true)
nano.cors.enabled=true

# Origens permitidas (padrão: *)
nano.cors.allowed-origins=*

# Métodos HTTP permitidos (padrão: GET,POST,PUT,DELETE,OPTIONS)
nano.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS

# Cabeçalhos permitidos (padrão: Content-Type,Authorization,X-Requested-With,Accept)
nano.cors.allowed-headers=Content-Type,Authorization,X-Requested-With,Accept

# Tempo de cache em segundos do pre-flight (padrão: 86400)
nano.cors.max-age=86400
```

#### 2. Configuração programática:
```java
// Ajustar configurações em tempo de execução:
CorsConfiguration cors = server.getCorsConfiguration();
```

### 14. Módulo Enterprise: Resiliência, Concorrência & Observabilidade (v1.7.0)

O Nano-Spring inclui endurecimento de infraestrutura para dispositivos embarcados em produção (PDVs, totens e hubs IoT):

#### A. Pool de Threads Controlado (`ThreadPoolAsyncRunner` - Anti-OOM)
Elimina o risco de esgotamento de threads nativas do Linux no Android através de um executor delimitado:
```properties
nano.server.threads.core=4
nano.server.threads.max=16
nano.server.threads.queue-capacity=100
nano.server.threads.keep-alive=60
```

#### B. Concorrência SQLite com Modo WAL (Write-Ahead Logging)
Permite que múltiplas requisições façam leituras em paralelo sem serem travadas por transações de escrita:
```properties
nano.datasource.wal.enabled=true
```

#### C. Monitoramento Dinâmico de Rede (`NetworkWatcher`)
Detecta alterações de IP / Wi-Fi via `ConnectivityManager.NetworkCallback` e re-anuncia automaticamente os serviços mDNS sem reiniciar a aplicação:
```properties
nano.network.watcher.enabled=true
```

#### D. Endpoints de Observabilidade Actuator (`/actuator/health` e `/actuator/info`)
Telemetria remota em formato JSON contendo memória JVM, conectividade do banco, IP/porta e métricas de bateria do Android:
```bash
curl http://localhost:8080/actuator/health
```

### 15. Segurança Corporativa & Validação Declarativa (v1.8.0)

#### A. HTTPS / TLS Nativo via Keystore
Habilite criptografia TLS no servidor configurando o Keystore no `application.properties`:
```properties
server.ssl.enabled=true
server.ssl.key-store=certificates/keystore.p12
server.ssl.key-store-password=senha-do-certificado
server.ssl.key-store-type=PKCS12
```

#### B. Proteção Anti-DoS com Rate Limiting
Limite o fluxo de requisições por IP globalmente ou em rotas críticas com `@RateLimit`:
```java
@RateLimit(requests = 5, durationSeconds = 60)
@PostMethod("/api/checkout")
public ResponseEntity<?> pagar(@RequestBody PagamentoDTO dto) { ... }
```
Se excedido, responde imediatamente com status HTTP `429 Too Many Requests`.

#### C. Bean Validation Declarativo (`@Valid`)
Valide dados de entrada com anotações declarativas no DTO:
```java
public class UsuarioDTO {
    @NotNull @NotBlank
    private String nome;

    @Email
    private String email;

    @Min(18) @Max(120)
    private int idade;
}
```
No controlador:
```java
@PostMethod("/api/usuarios")
public ResponseEntity<?> criar(@Valid @RequestBody UsuarioDTO dto) {
    // Executa apenas se o payload for 100% válido; caso contrário retorna 400 Bad Request
    return ResponseEntity.ok(service.salvar(dto));
}
```

### 16. Diagnósticos de Campo, Perfis & Execução Assíncrona (v1.9.0)

#### A. Rotação Local de Logs & Endpoint `/actuator/logfile`
Persistência de logs no armazenamento interno do Android sem risco de esgotar a memória interna do equipamento (rotação geracional com teto de tamanho):
```properties
nano.logging.enabled=true
nano.logging.level=INFO
nano.logging.max-size-mb=5
nano.logging.max-history=3
```
Suporte técnico pode auditar o dispositivo remotamente via HTTP:
```bash
curl http://terminal-pos.local:8080/actuator/logfile?lines=100
```

#### B. Perfis de Ambiente (`application-{profile}.properties`)
Segregação transparente de configurações por ambiente:
```properties
# assets/application.properties
nano.profiles.active=dev
```
O Nano-Spring carrega e sobrepõe automaticamente as chaves de `assets/application-dev.properties`.

#### C. Controladores Assíncronos com `CompletableFuture`
Desacople chamadas de hardware lentas (impressão térmica, leitura de chip EMV, Bluetooth) com timeout automático (`408 Request Timeout`):
```java
@PostMethod("/api/pos/imprimir")
public CompletableFuture<ResponseEntity<Map<String, Object>>> imprimir(@RequestBody CupomDTO cupom) {
    return CompletableFuture.supplyAsync(() -> {
        impressoraHardware.imprimir(cupom);
        return ResponseEntity.ok(Map.of("impresso", true));
    });
}
```
Configuração de timeout no `application.properties`:
```properties
nano.async.timeout-seconds=30
nano.server.read-timeout=10000
```

### 17. Swagger & OpenAPI 3.0 Embutido (v1.10.0)

Documente e teste suas rotas interativamente sem precisar instalar pacotes NPM ou bibliotecas pesadas de 8 MB+. O **Nano-Swagger** gera um endpoint OpenAPI 3.0.1 dinâmico e serve uma interface HTML5/CSS/JS ultra-leve (~15 KB) que funciona 100% offline em redes locais de TV-Box ou terminais POS:

- **Interface Gráfica Interativa:** `http://<ip-do-dispositivo>:8080/swagger-ui` (ou `/swagger-ui.html`)
- **Especificação OpenAPI JSON:** `http://<ip-do-dispositivo>:8080/v3/api-docs`

#### Exemplo de Controller Documentado:
```java
@Tag(name = "Produtos", description = "Operações do catálogo de produtos")
@RestController("/api/produtos")
public class ProdutoController {

    @Operation(summary = "Consultar produto por código de barras")
    @GetMethod("/:ean")
    public ResponseEntity<ProdutoDTO> buscarPorEan(
            @PathVariable("ean") @Parameter(description = "EAN-13 numérico", example = "7891000100103") String ean
    ) {
        return ResponseEntity.ok(produtoService.buscarPorEan(ean));
    }

    @Operation(summary = "Cadastrar novo produto")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = 201, description = "Criado com sucesso", responseClass = ProdutoDTO.class)
    @ApiResponse(responseCode = 400, description = "Validação falhou")
    @PostMethod("")
    public ResponseEntity<ProdutoDTO> cadastrar(@Valid @RequestBody ProdutoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.salvar(dto));
    }
}
```

Configuração opcional em `application.properties`:
```properties
nano.swagger.enabled=true
nano.swagger.title=PDV Smart API
nano.swagger.version=1.0.0
nano.swagger.description=API REST de Vendas e Integração Local
```

---

## ⚖️ Comparativo: Nano-Spring vs Spring Boot no Android

Por que não rodar o Spring Boot tradicional diretamente no Android? E como o Nano-Spring se compara rodando uma mesma estrutura de projeto?

| Dimensão / Métrica | 🌱 Nano-Spring (Android Nativo) | 🍃 Spring Boot (Stack Padrão) |
| :--- | :--- | :--- |
| **Consumo de Memória RAM** | **15 MB a 40 MB** *(Sem risco de OOM / LMK)* | **250 MB a 650 MB+** *(Morte iminente em 1GB-2GB RAM)* |
| **Tempo de Inicialização (Cold Start)** | **~150 ms a 400 ms** *(Escaneamento DexFile)* | **8 a 25+ segundos** *(Auto-configurations pesadas em ARM)* |
| **Impacto no APK / Footprint** | **&lt; 350 KB** *(Biblioteca enxuta)* | **35 MB a 70 MB** *(Fat JAR com centenas de libs transitivas)* |
| **Execução no Android Runtime (ART)** | **100% Nativo** *(Compila com D8/R8 sem hacks)* | **Incompatível Nativamente** *(Requer Termux / Proot / JVM hack)* |
| **Ciclo de Vida & Background Locks** | **Nativo** (`NanoSpringService`, WakeLock, Multicast) | **Sem integração com ciclo de vida do Android** |
| **Banco de Dados Embutido** | **SQLite Nativo do Android** *(WAL mode sem JNI)* | **H2 / SQLite via JDBC JNI de desktop** |
| **Descoberta na Rede Local (mDNS)** | **Nativo via NsdManager** (`http://device.local`) | **Requer JmDNS externo e gerenciamento manual de locks** |
| **Google Play Store / Segurança MDM** | **100% Compliant** | **Rejeição por execução de binários Linux fora da sandbox** |

### Ergonomia de Código Lado a Lado

Desenvolvedores habituados ao Spring Boot têm **curva de aprendizado zero** ao migrar ou desenvolver para o Nano-Spring:

```java
// 🍃 Spring Boot (Cloud / Desktop)
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    @Autowired
    private PedidoService service;

    @PostMapping
    public ResponseEntity<Pedido> criar(@Valid @RequestBody PedidoDTO dto,
                                        @RequestHeader("Authorization") String token) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(dto));
    }
}

// 🌱 Nano-Spring (Android Embarcado)
@RestController("/api/pedidos")
public class PedidoController {
    @Autowired
    private PedidoService service;

    @PostMethod("")
    public ResponseEntity<Pedido> criar(@Valid @RequestBody PedidoDTO dto,
                                        @RequestHeader("Authorization") String token) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(dto));
    }
}
```

> 📖 Para a análise aprofundada completa com matriz de decisão e gráficos, consulte a [Página de Comparativo Arquitetural na Documentação](docs/spring-boot-vs-nano-spring.html).

---

## 🛠️ Como funciona por baixo dos panos?

O Nano-Spring utiliza um mecanismo altamente otimizado de **introspecção via Java Reflection** e escaneamento direto no bytecode compilado do Android (`DexFile`), concentrado exclusivamente na inicialização (*boot phase*).

Em vez de processar milhares de classes de bibliotecas externas, o framework inspeciona cirurgicamente apenas o pacote base delimitado por você, identificando anotações como `@RestController`, `@Service` e `@Repository`. Os componentes Singleton são instanciados e todas as rotas e injeções de dependência são pré-compiladas e indexadas em tabelas de dispersão (*Hash Maps*) em memória.

Com isso, o custo de reflexão ocorre **uma única vez no arranque** (completando em cerca de 150 a 400 ms). Durante as requisições HTTP em tempo de execução, o despacho de rotas é praticamente instantâneo ($O(1)$), e o `Gson` converte fluxos de dados diretamente para seus modelos Java — proporcionando máxima vazão com consumo mínimo de memória e CPU no dispositivo.

---
**Criado com ♥ para a comunidade Android.**
