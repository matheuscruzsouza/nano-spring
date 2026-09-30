# 🔒 Interceptadores e Tratamento Global de Exceções

O **Nano-Spring** permite interceptar requisições para controle de acesso, auditoria e autenticação, além de centralizar a captura de exceções em toda a aplicação.

---

## 🛡️ Interceptadores (`HandlerInterceptor`)

Interceptadores funcionam como middlewares que avaliam a requisição antes que ela atinja o método do `@RestController`.

### 1. Criando um Interceptador

Para criar um interceptador:
1. Implemente a interface `HandlerInterceptor`.
2. Anote a classe com `@Interceptor`.
3. (Opcional) Anote com `@Order(int)` para definir a prioridade (números menores executam primeiro).

```java
package com.exemplo.meuapp.security;

import com.github.matheuscruzsouza.nanospring.annotation.Interceptor;
import com.github.matheuscruzsouza.nanospring.annotation.Order;
import com.github.matheuscruzsouza.nanospring.handler.HandlerInterceptor;
import fi.iki.elonen.NanoHTTPD;

@Interceptor
@Order(1) // Executa primeiro na esteira
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(NanoHTTPD.IHTTPSession session, String path) {
        // Rotas públicas liberadas
        if (path.startsWith("/api/public") || path.startsWith("/static")) {
            return true;
        }

        // Validação de Token Bearer em rotas protegidas
        if (path.startsWith("/api/admin")) {
            String authHeader = session.getHeaders().get("authorization");
            if (authHeader == null || !authHeader.equals("Bearer TOKEN_SECRETO")) {
                // Retornar false bloqueia a requisição e devolve 401 Unauthorized
                return false;
            }
        }

        return true; // Continua a execução
    }
}
```

### 2. Interceptador de Log / Auditoria

```java
package com.exemplo.meuapp.interceptor;

import android.util.Log;
import com.github.matheuscruzsouza.nanospring.annotation.Interceptor;
import com.github.matheuscruzsouza.nanospring.annotation.Order;
import com.github.matheuscruzsouza.nanospring.handler.HandlerInterceptor;
import fi.iki.elonen.NanoHTTPD;

@Interceptor
@Order(2)
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String TAG = "HTTP_AUDIT";

    @Override
    public boolean preHandle(NanoHTTPD.IHTTPSession session, String path) {
        Log.i(TAG, "[" + session.getMethod() + "] " + path + " de " + session.getRemoteIpAddress());
        return true;
    }
}
```

---

## 🚨 Tratamento Global de Exceções (`@ControllerAdvice`)

Para evitar blocos `try-catch` repetitivos nos controllers e garantir respostas JSON elegantes e padronizadas em caso de erro, utilize um Advice:

```java
package com.exemplo.meuapp.exception;

import com.github.matheuscruzsouza.nanospring.annotation.ControllerAdvice;
import com.github.matheuscruzsouza.nanospring.annotation.ExceptionHandler;
import com.github.matheuscruzsouza.nanospring.http.HttpStatus;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Trata erros de conversão de parâmetros numéricos (ex: letras passadas no id)
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Map<String, Object>> handleNumberFormatException(NumberFormatException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "erro", "Parâmetro numérico inválido",
                        "detalhe", ex.getMessage()
                ));
    }

    // Trata qualquer outra exceção não capturada especificamente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", 500,
                        "erro", "Erro interno do servidor",
                        "mensagem", ex.getMessage() != null ? ex.getMessage() : "Erro inesperado"
                ));
    }
}
```

[⬅ Voltar para o Índice](../index.md)
