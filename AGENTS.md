# Regras para Assistentes de IA (AGENTS.md)

Se você é uma IA, LLM, Copilot ou agente atuando neste repositório ou escrevendo código para este framework, **DEVE ler e seguir estas instruções.**

## 1. Identidade do Framework
Você está trabalhando com o **Nano-Spring**.
- **O Nano-Spring NÃO é o Spring Boot.** 
- É um framework próprio desenvolvido para rodar localmente no sistema operacional Android.
- Você **nunca** deve usar pacotes do `org.springframework.*`. 
- Use apenas pacotes que começam com `com.github.matheuscruzsouza.nanospring.*`.

## 2. Onde Encontrar Contexto e Exemplos
Se você não sabe como escrever um controller, criar uma classe de serviço, se conectar ao banco de dados ou validar campos, não invente sintaxe e não alucine pacotes. 

Consulte a documentação dedicada para AIs na pasta:
👉 `docs/ai/`

- `docs/ai/00-nano-spring-rules.md`: Regras gerais do framework.
- `docs/ai/01-core-di.md`: Injeção de dependência (`@Service`, `@Autowired`).
- `docs/ai/02-web-controllers.md`: Roteamento (`@RestController`, `@GetMethod`).
- `docs/ai/03-data-sqlite.md`: Banco de dados (`@Repository`, SQLite).
- `docs/ai/04-validation.md`: Validação de DTOs (`@Valid`, `@NotNull`).
- `docs/ai/05-async-sse.md`: Concorrência e eventos (`SseEmitter`).

Sempre leia esses arquivos pequenos antes de gerar o código final.

## 3. Diretrizes Comportamentais 
Adicionalmente, leia o arquivo `GEMINI.md` para regras de comportamento de código (Mantenha o código simples, mudanças cirúrgicas e focado no objetivo).
