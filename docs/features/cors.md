# 🌐 CORS Nativo Declarativo

O **Nano-Spring** inclui suporte nativo a **Cross-Origin Resource Sharing (CORS)**, ativo por padrão com configurações seguras e permissivas para desenvolvimento rápido.

Ele elimina a necessidade de interceptadores manuais para lidar com requisições *pre-flight* (`OPTIONS`) de SPAs (React, Vue, Angular, Svelte) e aplicativos web externos.

---

## ⚡ Comportamento Padrão

* **Ativado por padrão:** Não exige configuração manual para funcionar.
* **Pre-flight automático:** Requisições com método `OPTIONS` retornam instantaneamente `200 OK` (corpo vazio) com todos os cabeçalhos `Access-Control-*`, sem acionar a rota de negócio.
* **Injeção global:** Os cabeçalhos CORS são injetados em todas as respostas (sucesso, arquivos estáticos, SSE e erros disparados por `@ControllerAdvice`).
* **Zero overhead:** Se desativado, há apenas uma verificação booleana no fluxo e nenhum cabeçalho extra é processado.

---

## ⚙️ Configuração via `application.properties`

Personalize o comportamento adicionando ao `assets/application.properties`:

```properties
# Habilita ou desabilita o filtro global de CORS (padrão: true)
nano.cors.enabled=true

# Origens permitidas separadas por vírgula (padrão: *)
nano.cors.allowed-origins=http://localhost:3000,https://meuapp.com

# Métodos HTTP permitidos (padrão: GET,POST,PUT,DELETE,OPTIONS)
nano.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS

# Cabeçalhos permitidos na requisição (padrão: Content-Type,Authorization,X-Requested-With,Accept)
nano.cors.allowed-headers=Content-Type,Authorization,X-Requested-With,Accept,X-Custom-Token

# Tempo em segundos que o navegador pode manter em cache a resposta pre-flight (padrão: 86400 = 24h)
nano.cors.max-age=86400
```

---

## 💻 Customização Programática

Você também pode alterar as diretivas de CORS em tempo de execução via instância do `Server`:

```java
import com.github.matheuscruzsouza.nanospring.cors.CorsConfiguration;

// Obtém a configuração de CORS do servidor
CorsConfiguration cors = server.getCorsConfiguration();

// Configura origens e cabeçalhos permitidos dinamicamente
cors.setAllowedOrigins("https://dashboard.meuapp.com");
cors.setAllowedHeaders("Content-Type,Authorization,X-Api-Key");
```

---

## 📦 Cabeçalhos Injetados

Quando o CORS está ativo, o Nano-Spring adiciona automaticamente à resposta:

```http
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
Access-Control-Allow-Headers: Content-Type,Authorization,X-Requested-With,Accept
Access-Control-Max-Age: 86400
```

[⬅ Voltar para o Índice](../index.md)
