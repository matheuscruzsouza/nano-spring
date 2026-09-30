# 📡 Server-Sent Events (SSE) em Tempo Real

O **Nano-Spring** suporta **Server-Sent Events (SSE)** através da classe `SseEmitter`, permitindo transmitir fluxos de eventos e dados unidirecionais contínuos do Android para navegadores ou clientes HTTP em tempo real, sobre uma única conexão HTTP persistente.

Ideal para dashboards, telemetria de sensores do celular, progresso de downloads e notificações push locais.

---

## 🛠️ Implementando no Controller

Basta retornar uma instância de `SseEmitter` e definir o `mimeType = "text/event-stream"` na anotação `@GetMethod`:

```java
package com.exemplo.meuapp.controller;

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.sse.SseEmitter;
import java.util.Map;

@RestController("/api/stream")
public class TelemetriaController {

    @GetMethod(value = "/bateria", mimeType = "text/event-stream")
    public SseEmitter streamStatusBateria() {
        SseEmitter emitter = new SseEmitter();

        // Dispara uma thread para alimentar os eventos em background
        new Thread(() -> {
            try {
                emitter.send("Conexão SSE estabelecida com sucesso!");

                for (int i = 0; i < 10; i++) {
                    Thread.sleep(1000);

                    // Envia dados estruturados como JSON
                    Map<String, Object> dados = Map.of(
                            "timestamp", System.currentTimeMillis(),
                            "nivelBateria", 85 - (i * 2),
                            "carregando", false
                    );

                    // Pode enviar mensagens padrão ou eventos nomeados:
                    emitter.send("leitura_sensor", dados);
                }

                // Finaliza o stream normalmente
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}
```

---

## 🌐 Consumindo no Frontend

### 1. JavaScript Nativo (`EventSource`)

```javascript
// Conecta no endpoint SSE
const eventSource = new EventSource('http://meu-servidor.local:8080/api/stream/bateria');

// Recebe mensagens genéricas (sem nome de evento)
eventSource.onmessage = (event) => {
    console.log("Mensagem recebida:", event.data);
};

// Ouve eventos específicos emitidos com emitter.send("leitura_sensor", dados)
eventSource.addEventListener('leitura_sensor', (event) => {
    const sensor = JSON.parse(event.data);
    console.log("Nível da Bateria:", sensor.nivelBateria + "%");
});

// Tratamento de encerramento ou erros
eventSource.onerror = (error) => {
    console.warn("Conexão encerrada pelo servidor ou erro:", error);
    eventSource.close();
};
```

---

### 2. Com HTMX (`hx-ext="sse"`)

O SSE do Nano-Spring é 100% compatível com a extensão SSE do HTMX:

```html
<div hx-ext="sse" sse-connect="/api/stream/bateria" sse-swap="leitura_sensor">
    Carregando leituras do sensor...
</div>
```

---

## 💡 Métodos de `SseEmitter`

| Método | Descrição |
| :--- | :--- |
| `emitter.send(Object data)` | Serializa o objeto para JSON e emite como evento padrão |
| `emitter.send(String eventName, Object data)` | Emite um bloco com `event: <eventName>\ndata: <json>\n\n` |
| `emitter.complete()` | Finaliza a resposta HTTP e fecha o fluxo limpo |
| `emitter.completeWithError(Throwable t)` | Fecha o fluxo sinalizando o erro |

[⬅ Voltar para o Índice](../index.md)
