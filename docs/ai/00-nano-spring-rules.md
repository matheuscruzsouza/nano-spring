# Regras Gerais do Nano-Spring para Agentes e IAs

Você está operando em um projeto que utiliza **Nano-Spring**, um framework Java/Kotlin desenhado para rodar serviços locais **diretamente em dispositivos Android**.

## Regra de Ouro (MUITO IMPORTANTE)
**NUNCA importe pacotes do Spring Framework padrão (`org.springframework.*`).**
O Nano-Spring é *inspirado* no Spring, mas é uma implementação própria focada em Android.
Todos os pacotes do Nano-Spring pertencem à base: `com.github.matheuscruzsouza.nanospring.*`.

## Contexto do Projeto
- O projeto é **Offline-First**. Assuma que não há conexão confiável com a Internet (não use APIs de terceiros na nuvem).
- Restrição de recursos: Código gerado deve ser eficiente em memória e não deve criar infinitas threads. Use concorrência controlada.
- Plataforma base: Android (API 25+). Você pode usar as APIs padrão do Java (ex: `java.util`, `java.io`, `java.net`, `java.util.concurrent`) e as anotações fornecidas pelo Nano-Spring.

Se você precisa criar componentes específicos, consulte os outros arquivos nesta pasta `docs/ai/` para obter a sintaxe correta.
