# Controllers Assíncronos e Server-Sent Events (SSE)

Para operações lentas em hardware (como impressão ou comunicação com periféricos via Bluetooth/USB), o Nano-Spring suporta a execução assíncrona baseada em `CompletableFuture` e `SseEmitter`.

## Controllers Assíncronos
Use `CompletableFuture` como tipo de retorno no controller.
Não é necessário usar anotações como `@Async`, apenas retornar o future.

```java
import com.github.matheuscruzsouza.nanospring.annotation.*;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

@RestController("/api/pos")
public class PosController {

    @PostMethod("/imprimir")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> imprimir(@RequestBody CupomDTO cupom) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Operação bloqueante na impressora
            impressoraHardware.imprimir(cupom);
            
            return ResponseEntity.ok(Map.of("impresso", true));
        });
    }
}
```

Configurações de timeout (`application.properties`):
```properties
nano.async.timeout-seconds=30
nano.server.read-timeout=10000
```

## Server-Sent Events (SSE)
Para enviar fluxos de eventos contínuos para o cliente web.

```java
import com.github.matheuscruzsouza.nanospring.annotation.*;
// Use a classe SseEmitter do Nano-Spring (não do org.springframework.*)

@RestController("/api/stream")
public class NotificationController {

    @GetMethod(
            value = "/alerts",
            mimeType = "text/event-stream"
    )
    public SseEmitter alerts() {

        SseEmitter emitter = new SseEmitter();
        // Lógica para produzir eventos assincronamente...
        
        return emitter;
    }
}
```
