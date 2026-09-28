# Nano-Spring 🌱

**Nano-Spring** é um micro-framework web ultra-leve construído exclusivamente para **Android**. Ele traz a elegância, produtividade e o padrão arquitetural do **Spring Boot** para dentro do ambiente móvel, utilizando o NanoHTTPD por baixo dos panos.

Com o Nano-Spring, você transforma qualquer celular ou tablet Android em um poderoso servidor web (Frontend e Backend), escrevendo código limpo, sem boilerplate e focado em anotações.

---

## 🌟 Principais Funcionalidades

- 🚀 **Roteamento RESTful:** `@RestController`, `@GetMethod`, `@PostMethod`, `@PutMethod`, `@DeleteMethod`.
- 💉 **Injeção de Dependências (DI):** Gerenciamento automático de ciclo de vida com `@Service` e `@Autowired`.
- ⚙️ **Configurações Externalizadas:** Suporte nativo a `application.properties` e injeção via `@Value`.
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

No arquivo `build.gradle` do seu aplicativo principal (`app`), importe o módulo do Nano-Spring:
```gradle
dependencies {
    implementation project(':nano-spring')
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

### 6. Interceptadores (Segurança e Logs)
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

### 7. Tratamento de Exceções Globais
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

### 8. Retornando Páginas HTML e HTMX
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

### 9. Arquivos Estáticos (CSS, JS, Imagens)
Coloque qualquer arquivo estático dentro de `app/src/main/assets/static/`.
O Nano-Spring hospeda essa pasta automaticamente! No seu HTML, basta linkar:
```html
<link rel="stylesheet" href="/static/css/style.css">
<script src="/static/js/main.js"></script>
```

---

## 🛠️ Como funciona por baixo dos panos?
O Nano-Spring usa um sistema pesado de **Java Reflection** no momento em que o servidor sobe (Boot phase). Ele varre o pacote que você especificou, procura pelas anotações (como `@RestController` e `@Service`), instancia as classes Singleton e mapeia as rotas e dependências em memória. 

Durante as requisições, ele usa *Regex* para casar URLs dinâmicas e o `Gson` para converter Streams de JSON diretamente nos seus modelos Java, assim como o ecossistema Spring Boot.

---
**Criado com ♥ para a comunidade Android.**
