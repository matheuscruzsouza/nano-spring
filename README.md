# Nano-Spring 🌱

**Nano-Spring** é um micro-framework web ultra-leve construído exclusivamente para **Android**. Ele traz a elegância, produtividade e o padrão arquitetural do **Spring Boot** para dentro do ambiente móvel, utilizando o NanoHTTPD por baixo dos panos.

Com o Nano-Spring, você transforma qualquer celular ou tablet Android em um poderoso servidor web (Frontend e Backend), escrevendo código limpo, sem boilerplate e focado em anotações.

---

## 🌟 Principais Funcionalidades

- 🚀 **Roteamento RESTful:** `@RestController`, `@GetMethod`, `@PostMethod`, `@PutMethod`, `@DeleteMethod`.
- 💉 **Injeção de Dependências (DI):** Gerenciamento automático de ciclo de vida com `@Service`, `@Repository` e `@Autowired`.
- 🗄️ **Banco de Dados SQLite & Migrações:** Suporte nativo ao SQLite com versionamento de esquemas por arquivos SQL (estilo Flyway).
- 🔍 **Descoberta de Serviço (mDNS / DNS-SD):** Anúncio automático do servidor na rede local via Zeroconf / Bonjour com NsdManager nativo.
- ⚙️ **Configurações Externalizadas:** Suporte nativo a `application.properties` e injeção via `@Value`.
- 📡 **Server-Sent Events (SSE):** Streaming de eventos unidirecionais em tempo real para navegadores com `SseEmitter`.
- 📁 **Upload de Arquivos Nativo:** Receba imagens e documentos facilmente via *multipart/form-data* usando `@UploadedFile`.
- 🛡️ **Middlewares (Interceptadores):** Intercepte requisições nativamente para validação de JWTs, logs e segurança.
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
    implementation 'com.github.matheuscruzsouza:nano-spring:1.5.0'
}
```

O Nano-Spring já encapsula as seguintes dependências:
- `org.nanohttpd:nanohttpd:2.3.1`
- `com.google.code.gson:gson:2.8.9`
- `com.samskivert:jmustache:1.15`

---

## 🚀 Guia de Uso Completo e Exemplos

### 1. Inicializando o Servidor
Crie um serviço no Android para rodar o servidor em Background. O servidor mapeia automaticamente seus controladores:

```java
import com.github.matheuscruzsouza.nanospring.server.Server;

public class MyBackendService extends android.app.Service {
    private Server server;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Inicializa na porta 8080 (ou a definida no application.properties)
        server = new Server(this, 8080, "com.seupacote.app");
        return START_STICKY;
    }
}
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

    // Acessível via GET /api/users/123
    @GetMethod("/:id")
    public User getUser(@PathVariable("id") String id) {
        return userService.findById(Integer.parseInt(id));
    }

    // Acessível via POST /api/users com JSON no Body
    @PostMethod("")
    public User createUser(@RequestBody User newUser) {
        return userService.save(newUser);
    }
}
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

---

## 🛠️ Como funciona por baixo dos panos?
O Nano-Spring usa um sistema pesado de **Java Reflection** no momento em que o servidor sobe (Boot phase). Ele varre o pacote que você especificou, procura pelas anotações (como `@RestController` e `@Service`), instancia as classes Singleton e mapeia as rotas e dependências em memória. 

Durante as requisições, ele usa *Regex* para casar URLs dinâmicas e o `Gson` para converter Streams de JSON diretamente nos seus modelos Java, assim como o ecossistema Spring Boot.

---
**Criado com ♥ para a comunidade Android.**
