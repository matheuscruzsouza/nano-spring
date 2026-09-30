# Nano-Spring 🌱

> **Micro-framework web ultra-leve para Android com a elegância e produtividade do Spring Boot.**

O **Nano-Spring** transforma qualquer smartphone, tablet ou dispositivo embarcado Android em um servidor web robusto (REST API, banco de dados local e renderização HTML), consumindo apenas entre **15 MB e 40 MB de memória RAM**.

---

## ⚡ Início Rápido (3 Passos)

### 1. Adicione a dependência
No arquivo `build.gradle` do seu aplicativo Android (`app`):

```groovy
dependencies {
    implementation 'com.github.matheuscruzsouza:nano-spring:1.6.0'
}
```

### 2. Inicie o servidor em um Service Android
```java
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.github.matheuscruzsouza.nanospring.server.Server;

public class AppServerService extends Service {
    private Server server;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Inicializa o servidor na porta 8080 escaneando as anotações do seu pacote
        server = new Server(this, 8080, "com.exemplo.meuapp");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (server != null) server.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
```

### 3. Crie o seu primeiro Controller REST
```java
package com.exemplo.meuapp.controllers;

import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import java.util.Map;

@RestController("/api")
public class HelloController {

    @GetMethod("/ola")
    public Map<String, String> dizerOla() {
        return Map.of(
            "status", "online",
            "mensagem", "Servidor rodando nativamente no Android!"
        );
    }
}
```

Pronto! Acesse pelo navegador ou cliente HTTP: `http://localhost:8080/api/ola` (ou `http://<ip-do-celular>:8080/api/ola`).

---

## 📚 Documentação das Funcionalidades

Consulte as páginas detalhadas com exemplos completos de cada recurso:

| Recurso | Descrição | Documentação |
| :--- | :--- | :--- |
| 🚀 **Instalação e Setup** | Requisitos mínimos, permissões e setup via GitHub Packages ou módulo local | [Ver Guia](getting-started.md) |
| 🌐 **APIs RESTful** | `@RestController`, verbos HTTP, parâmetros de rota, query e `ResponseEntity` | [Ver Página](features/rest-api.md) |
| 💉 **Injeção de Dependências** | `@Service`, `@Repository`, `@Autowired` e `@Value` com `application.properties` | [Ver Página](features/dependency-injection.md) |
| 🗄️ **SQLite & Migrações** | Banco de dados local com versionamento automático de esquemas (estilo Flyway) | [Ver Página](features/sqlite-migrations.md) |
| 🔍 **Descoberta mDNS / DNS-SD** | Anúncio Zeroconf e resolução direta no navegador via `<servidor>.local` | [Ver Página](features/mdns-discovery.md) |
| 🛡️ **CORS Nativo** | Suporte declarativo a Cross-Origin com resolução automática de `OPTIONS` | [Ver Página](features/cors.md) |
| 📡 **Server-Sent Events (SSE)** | Streaming de eventos unidirecionais em tempo real com `SseEmitter` | [Ver Página](features/sse.md) |
| 📁 **Upload & Download de Arquivos** | Recebimento via `@UploadedFile` e download em blocos (chunked) | [Ver Página](features/files.md) |
| 🔒 **Interceptadores & Erros** | Filtros globais com `@Order` e captura de exceções via `@ControllerAdvice` | [Ver Página](features/interceptors-errors.md) |
| 🎨 **Templates HTML & Estáticos** | Renderização server-side com JMustache (`ModelAndView`) e arquivos estáticos | [Ver Página](features/templates-static.md) |

---

## 📱 Requisitos do Sistema

- **Android Mínimo:** Android 7.1.1 (API 25 / Nougat)
- **Compilado com:** API 34+ (compileSdk 36)
- **Java:** Java 11+
- **Memória RAM:** 15 MB a 40 MB em uso típico
