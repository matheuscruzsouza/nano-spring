# 💉 Injeção de Dependências e Configurações

O **Nano-Spring** inclui um container leve de Injeção de Dependências (IoC/DI) que gerencia o ciclo de vida dos componentes da sua aplicação em formato **Singleton**, sem requerer bibliotecas pesadas.

---

## 📌 Anotações de Componentes

| Anotação | Uso | Descrição |
| :--- | :--- | :--- |
| `@Service` | Classes de Regra de Negócio | Registra a classe como um serviço singleton |
| `@Repository` | Classes de Acesso a Dados | Registra a classe como um repositório DAO/SQLite |
| `@Autowired` | Campos | Injeta automaticamente uma dependência singleton registrada |
| `@Value` | Campos | Injeta valores de propriedades de `application.properties` |

---

## 💡 Exemplo de Arquitetura em Camadas

### 1. Camada de Repositório (`@Repository`)

```java
package com.exemplo.meuapp.repository;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ItemRepository {

    @Autowired
    private SQLiteDatabase db;

    public List<String> listAllNames() {
        List<String> result = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("SELECT name FROM items", null)) {
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0));
            }
        }
        return result;
    }
}
```

### 2. Camada de Serviço (`@Service`)

```java
package com.exemplo.meuapp.service;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.Service;
import com.exemplo.meuapp.repository.ItemRepository;
import java.util.List;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    public List<String> getFormattedItems() {
        return itemRepository.listAllNames();
    }
}
```

### 3. Camada de Controle (`@RestController`)

```java
package com.exemplo.meuapp.controller;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.exemplo.meuapp.service.ItemService;
import java.util.List;

@RestController("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMethod("")
    public List<String> getAll() {
        return itemService.getFormattedItems();
    }
}
```

---

## ⚙️ Configurações Externalizadas com `application.properties`

Coloque um arquivo chamado `application.properties` dentro da pasta `app/src/main/assets/`:

```properties
# app/src/main/assets/application.properties
server.port=9090
app.empresa.nome=Minha Loja Android
app.limite.requisicoes=100
```

### Injetando propriedades com `@Value`

Você pode injetar valores com valores padrão de fallback usando a sintaxe `${chave:default}`:

```java
package com.exemplo.meuapp.controller;

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.annotation.Value;

@RestController("/api/info")
public class InfoController {

    // Se não encontrar "app.empresa.nome", usa "Empresa Padrão"
    @Value("${app.empresa.nome:Empresa Padrão}")
    private String nomeEmpresa;

    @Value("${app.limite.requisicoes:50}")
    private int limiteRequisicoes;

    @GetMethod("")
    public String getInfo() {
        return "Empresa: " + nomeEmpresa + " (Limite: " + limiteRequisicoes + ")";
    }
}
```

---

## 🔍 Como o Container Funciona

Durante a inicialização do `Server(context, port, packageName)`:
1. O framework localiza todas as classes anotadas com `@Service`, `@Repository` e `@RestController`.
2. Cria uma instância de cada classe (Singletons).
3. Percorre os campos anotados com `@Autowired` e resolve as dependências entre os componentes.
4. Percorre os campos com `@Value` e preenche a partir de `Environment.getProperties()`.

[⬅ Voltar para o Índice](../index.md)
