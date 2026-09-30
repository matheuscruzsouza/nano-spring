# 🎨 Templates HTML & Arquivos Estáticos

O **Nano-Spring** permite renderizar interfaces completas diretamente no servidor Android via **JMustache**, além de servir arquivos estáticos (CSS, JavaScript, Imagens e Fontes) automaticamente.

---

## 📄 Renderização Server-Side (`ModelAndView`)

### 1. Criando o Template HTML
Coloque seus arquivos HTML dentro de `app/src/main/assets/templates/`:

```html
<!-- app/src/main/assets/templates/perfil.html -->
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <title>{{titulo}}</title>
    <link rel="stylesheet" href="/static/css/estilo.css">
</head>
<body>
    <h1>Olá, {{usuario.nome}}!</h1>
    <p>Email: {{usuario.email}}</p>

    <h3>Suas Tarefas:</h3>
    <ul>
        {{#tarefas}}
        <li>{{.}}</li>
        {{/tarefas}}
    </ul>
</body>
</html>
```

### 2. Retornando no `@RestController`
Retorne uma instância de `ModelAndView` passando o nome do template (sem a extensão `.html`) e adicione as variáveis com `.addObject()`:

```java
package com.exemplo.meuapp.controller;

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PathVariable;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.ui.ModelAndView;
import java.util.List;
import java.util.Map;

@RestController("/web")
public class WebController {

    @GetMethod("/perfil/:id")
    public ModelAndView exibirPerfil(@PathVariable("id") String id) {
        return new ModelAndView("perfil")
                .addObject("titulo", "Painel do Usuário")
                .addObject("usuario", Map.of("nome", "Matheus", "email", "matheus@local.com"))
                .addObject("tarefas", List.of("Configurar servidor", "Testar mDNS", "Publicar versão 1.6.0"));
    }
}
```

---

## ⚡ Integração com HTMX

Para devolver fragmentos HTML dinâmicos sem recarregar a página inteira, retorne uma `String` definindo `mimeType = "text/html"`:

```java
@GetMethod(value = "/hora-atual", mimeType = "text/html")
public String horaAtual() {
    return "<div class='badge'>Hora no Android: " + new java.util.Date() + "</div>";
}
```

No HTML cliente:
```html
<button hx-get="/web/hora-atual" hx-target="#relogio">
    Atualizar Horário
</button>
<div id="relogio"></div>
```

---

## 📁 Servidor de Arquivos Estáticos

Qualquer arquivo colocado na pasta `app/src/main/assets/static/` é servido automaticamente pelo Nano-Spring sob o prefixo `/static/`:

```text
app/src/main/assets/
└── static/
    ├── css/
    │   └── estilo.css
    ├── js/
    │   └── app.js
    └── img/
        └── logo.png
```

### Como referenciar no HTML:
```html
<link rel="stylesheet" href="/static/css/estilo.css">
<script src="/static/js/app.js"></script>
<img src="/static/img/logo.png" alt="Logo">
```

O Nano-Spring detecta os tipos MIME corretos (`text/css`, `application/javascript`, `image/png`, etc.) e gerencia os fluxos de leitura dos assets do APK de forma transparente.

[⬅ Voltar para o Índice](../index.md)
