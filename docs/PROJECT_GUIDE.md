# Guia de Criação de Projetos com Nano-Spring 🌱

> **Público-alvo:** Desenvolvedores humanos e Agentes de IA (Copilot, Cursor, LLMs).  
> **Propósito:** Guia passo a passo definitivo para bootstrapping, configuração e desenvolvimento de novas aplicações locais em Android utilizando o framework Nano-Spring.

---

## ⚠️ Regra de Ouro (Crítico para IAs e Humanos)

> [!CAUTION]
> **O NANO-SPRING NÃO É O SPRING BOOT.**  
> - **NUNCA** importe pacotes `org.springframework.*`.  
> - **SEMPRE** use pacotes `com.github.matheuscruzsouza.nanospring.*`.  
> - O framework é projetado especificamente para rodar **dentro de um APK no SO Android**, em ambiente **Local-First / Offline-First**.

---

## 1. Visão Geral da Arquitetura

O Nano-Spring transforma dispositivos Android (celulares, tablets, POS, TV boxes) em servidores HTTP locais e nós de infraestrutura autônomos.

```
┌─────────────────────────────────────────────────────────────────┐
│                       Android OS Runtime                        │
├─────────────────────────────────────────────────────────────────┤
│  Foreground Service (NanoSpringService)                         │
│  └── HTTP Server Engine (NanoHTTPD embutido)                    │
│      ├── Router (@RestController, @GetMethod, @PostMethod)      │
│      ├── Inversão de Controle & DI (@Service, @Autowired)       │
│      ├── Resolução de Parâmetros (@RequestBody, @PathVariable) │
│      ├── Validação de DTOs (@Valid, @NotNull, @Size)            │
│      ├── Banco de Dados Local (SQLite Helper / @Repository)     │
│      └── Descoberta de Rede Local (Zeroconf / mDNS)             │
└─────────────────────────────────────────────────────────────────┘
```

### Módulos Disponíveis

| Módulo | Dependência Gradle | Finalidade |
| :--- | :--- | :--- |
| **Core** | `nano-spring-core` | IoC, Injeção de Dependências, ciclo de vida de beans e reflexão |
| **Web** | `nano-spring-web` | Servidor HTTP, anotações REST, serialização JSON, SSE, Auth |
| **Data** | `nano-spring-data` | Acesso e migrações SQLite para Android (`@Repository`) |
| **Discovery** | `nano-spring-discovery` | Registro e resolução mDNS/Zeroconf na rede local |

---

## 2. Configurando um Novo Projeto Android

### 2.1 Requisitos Mínimos
- **minSdk**: `25` (Android 7.1.1+)
- **compileSdk / targetSdk**: `34+` (recomendado `36`)
- **Java**: 11 ou 17

### 2.2 Configuração dos Repositórios (`settings.gradle` ou `settings.gradle.kts`)
Adicione o repositório Maven do GitHub Packages:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
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

### 2.3 Dependências no `app/build.gradle` (ou `app/build.gradle.kts`)

```kotlin
dependencies {
    val nanoSpringVersion = "2.4.1"

    // Módulos Obrigatórios para API REST
    implementation("com.github.matheuscruzsouza:nano-spring-core:$nanoSpringVersion")
    implementation("com.github.matheuscruzsouza:nano-spring-web:$nanoSpringVersion")

    // Módulos Opcionais
    implementation("com.github.matheuscruzsouza:nano-spring-data:$nanoSpringVersion")
    implementation("com.github.matheuscruzsouza:nano-spring-discovery:$nanoSpringVersion")

    // AndroidX & Foreground Service
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
```

### 2.4 Permissões no `AndroidManifest.xml`
Para garantir a execução estável do servidor em segundo plano e acesso à rede local:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.meuprojeto">

    <!-- Rede -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <!-- Execução em Segundo Plano & Notificações -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.AppCompat.Light.NoActionBar">

        <!-- Declaração do Foreground Service do Nano-Spring -->
        <service
            android:name=".service.AppBackendService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="dataSync" />

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## 3. Estrutura Recomendada de Pacotes

Organize seu projeto pelo pacote base fornecido na inicialização do servidor:

```text
com.example.meuprojeto/
│
├── controller/          # Endpoints HTTP (@RestController)
│   ├── OrderController.java
│   └── SystemController.java
│
├── service/             # Regras de Negócio e Background Service
│   ├── AppBackendService.java   # Extende NanoSpringService
│   ├── OrderService.java        # Anotado com @Service
│   └── ...
│
├── repository/          # Persistência de Dados (@Repository)
│   └── OrderRepository.java
│
├── dto/                 # Modelos de entrada/saída serializáveis
│   ├── CreateOrderRequest.java
│   └── OrderResponse.java
│
└── MainActivity.java    # Interface para ligar/desligar o servidor
```

---

## 4. Implementação Passo a Passo

### Passo 1: O Serviço de Inicialização (`NanoSpringService`)

A forma recomendada de rodar o Nano-Spring é herdando de `NanoSpringService`, que gerencia notificações de Foreground Service e WakeLock automaticamente:

```java
package com.example.meuprojeto.service;

import com.github.matheuscruzsouza.nanospring.service.NanoSpringService;

public class AppBackendService extends NanoSpringService {

    @Override
    protected String getBasePackage() {
        // Pacote raiz escaneado para registrar @Service, @RestController, @Repository
        return "com.example.meuprojeto";
    }

    @Override
    protected int getPort() {
        return 8080;
    }
}
```

> **Nota:** Alternativamente, para testes rápidos em Activities/Instrumentados, pode-se instanciar diretamente:
> ```java
> Server server = new Server(context, 8080, "com.example.meuprojeto");
> server.start();
> ```

### Passo 2: Camada de Negócio (`@Service` e `@Autowired`)

```java
package com.example.meuprojeto.service;

import com.github.matheuscruzsouza.nanospring.annotation.Service;
import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.example.meuprojeto.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    // Injeção via construtor (recomendada) ou via campo @Autowired
    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public String processOrder(String orderId) {
        return "Pedido " + orderId + " processado com sucesso!";
    }
}
```

### Passo 3: DTO com Validação (`@Valid`)

```java
package com.example.meuprojeto.dto;

import com.github.matheuscruzsouza.nanospring.annotation.validation.NotNull;
import com.github.matheuscruzsouza.nanospring.annotation.validation.Size;

public class CreateOrderRequest {

    @NotNull(message = "O ID do cliente é obrigatório")
    private String customerId;

    @NotNull(message = "A descrição não pode ser nula")
    @Size(min = 3, max = 100, message = "Descrição deve ter entre 3 e 100 caracteres")
    private String description;

    public CreateOrderRequest() {}

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
```

### Passo 4: Controller REST (`@RestController`)

```java
package com.example.meuprojeto.controller;

import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PostMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PathVariable;
import com.github.matheuscruzsouza.nanospring.annotation.RequestBody;
import com.github.matheuscruzsouza.nanospring.annotation.RequestParam;
import com.github.matheuscruzsouza.nanospring.annotation.validation.Valid;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;
import com.example.meuprojeto.service.OrderService;
import com.example.meuprojeto.dto.CreateOrderRequest;

import java.util.HashMap;
import java.util.Map;

@RestController("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMethod("/:id")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable("id") String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("status", "COMPLETED");
        return ResponseEntity.ok(response);
    }

    @PostMethod("")
    public ResponseEntity<String> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        String result = orderService.processOrder(request.getCustomerId());
        return ResponseEntity.ok(result);
    }

    @GetMethod("/search")
    public ResponseEntity<String> searchOrders(@RequestParam("term") String term) {
        return ResponseEntity.ok("Resultados para: " + term);
    }
}
```

### Passo 5: Inicialização a partir da `MainActivity`

```java
package com.example.meuprojeto;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.meuprojeto.service.AppBackendService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicia o servidor HTTP em background
        Intent serviceIntent = new Intent(this, AppBackendService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
```

---

## 5. Cheat Sheet de Anotações & Classes

| Conceito | Anotação / Classe | Pacote Correto |
| :--- | :--- | :--- |
| **Controlador** | `@RestController(path)` | `com.github.matheuscruzsouza.nanospring.annotation.RestController` |
| **Rotas HTTP** | `@GetMethod`, `@PostMethod`, `@PutMethod`, `@DeleteMethod` | `com.github.matheuscruzsouza.nanospring.annotation.*` |
| **Parâmetros** | `@PathVariable("name")`, `@RequestParam("name")`, `@RequestBody` | `com.github.matheuscruzsouza.nanospring.annotation.*` |
| **Resposta HTTP** | `ResponseEntity<T>` | `com.github.matheuscruzsouza.nanospring.http.ResponseEntity` |
| **Injeção / Beans** | `@Service`, `@Repository`, `@Autowired` | `com.github.matheuscruzsouza.nanospring.annotation.*` |
| **Validação** | `@Valid`, `@NotNull`, `@Size` | `com.github.matheuscruzsouza.nanospring.annotation.validation.*` |
| **Serviço Base** | `NanoSpringService` | `com.github.matheuscruzsouza.nanospring.service.NanoSpringService` |
| **Servidor Puro** | `Server` | `com.github.matheuscruzsouza.nanospring.server.Server` |
| **SSE / Eventos** | `SseEmitter` | `com.github.matheuscruzsouza.nanospring.sse.SseEmitter` |

---

## 6. Instruções Específicas para IAs e LLMs

Quando um usuário solicitar a criação de código, rotas ou módulos para um projeto Nano-Spring:

1. **Checagem de Pacotes:**
   - ❌ NUNCA gerar imports como `import org.springframework.web.bind.annotation.GetMapping;`
   - ❌ NUNCA gerar imports como `import org.springframework.stereotype.Service;`
   - ✅ SEMPRE usar `@GetMethod`, `@PostMethod` do pacote `com.github.matheuscruzsouza.nanospring.annotation.*`.

2. **Parâmetros de Rota:**
   - As rotas utilizam a sintaxe `/:parametro` (exemplo: `@GetMethod("/:id")`).
   - Resgate com `@PathVariable("id") String id`.

3. **Restrições de Ambiente (Hardware & Bateria):**
   - Dispositivos Android possuem limites severos de memória (RAM) e encerramento de processos pelo SO.
   - Não crie loops infinitos sem pausas ou threads desacopladas sem controle. Prefira `ScheduledExecutorService` ou serviços gerenciados.
   - Não assuma conectividade com internet externa. Arquiteturas devem ser desenhadas **offline-first**.

4. **Documentação Complementar:**
   - Para SSE e streaming: consulte `docs/ai/05-async-sse.md`.
   - Para SQLite e migrações: consulte `docs/ai/03-data-sqlite.md`.
   - Para monitoramento de bateria: consulte `docs/ai/06-battery-notifications.md`.
