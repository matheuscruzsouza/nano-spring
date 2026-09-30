# 🗄️ SQLite & Migrações de Banco de Dados

O **Nano-Spring** conta com suporte nativo ao **SQLite do Android** e inclui um mecanismo integrado de migrações versionadas de esquema, inspirado no consagrado padrão do **Flyway**.

---

## 🚀 Como Funciona

1. Você coloca seus scripts SQL em `assets/db/migration/`.
2. Ao subir o servidor, o `SqliteMigrator` verifica quais migrações já foram aplicadas na tabela interna `nano_schema_history`.
3. Todas as migrações novas são executadas em ordem crescente de versão dentro de uma transação segura.
4. Uma instância do `android.database.sqlite.SQLiteDatabase` é automaticamente aberta e registrada para injeção via `@Autowired`.

---

## 📁 Estrutura de Arquivos de Migração

Coloque os arquivos `.sql` na pasta de assets do seu projeto Android:
```text
app/src/main/assets/
└── db/
    └── migration/
        ├── V1__create_tables.sql
        ├── V2__add_users.sql
        └── V3__create_orders_table.sql
```

> ⚠️ **Regra de Nomenclatura:** Os arquivos **devem** seguir o padrão:
> `V<versão>__<descrição>.sql` com **dois underscores** (`__`) separando a versão da descrição.
> * Exemplo válido: `V1__init.sql`, `V2__add_status_column.sql`.
> * Exemplo inválido: `V1_init.sql`, `migracao1.sql`.

---

## 📝 Exemplos de Scripts SQL

### `V1__create_tables.sql`:
```sql
CREATE TABLE IF NOT EXISTS categorias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS produtos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    categoria_id INTEGER,
    titulo TEXT NOT NULL,
    preco REAL NOT NULL,
    FOREIGN KEY(categoria_id) REFERENCES categorias(id)
);
```

### `V2__add_users.sql`:
```sql
CREATE TABLE IF NOT EXISTS usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    senha_hash TEXT NOT NULL
);

INSERT INTO usuarios (username, senha_hash) VALUES ('admin', 'hash_admin_123');
```

---

## ⚙️ Configurações Opcionais (`application.properties`)

Você pode personalizar o nome do arquivo do banco e a pasta de migrações no arquivo `assets/application.properties`:

```properties
# Nome do arquivo de banco no armazenamento interno do app (padrão: nano_spring_app.db)
nano.datasource.name=minha_loja.db

# Pasta dentro de assets contendo os arquivos SQL (padrão: db/migration)
nano.datasource.migration.location=db/migration

# Habilita ou desabilita a execução automática de migrações (padrão: true)
nano.datasource.migration.enabled=true
```

---

## 💉 Injetando `SQLiteDatabase` em Repositórios

Com a classe anotada com `@Repository`, basta declarar o campo com `@Autowired`:

```java
package com.exemplo.meuapp.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProdutoRepository {

    @Autowired
    private SQLiteDatabase db;

    public long salvar(String titulo, double preco) {
        ContentValues values = new ContentValues();
        values.put("titulo", titulo);
        values.put("preco", preco);
        return db.insert("produtos", null, values);
    }

    public List<String> listarTitulos() {
        List<String> produtos = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("SELECT titulo FROM produtos", null)) {
            while (cursor.moveToNext()) {
                produtos.add(cursor.getString(0));
            }
        }
        return produtos;
    }
}
```

---

## 🛡️ Tabela de Controle: `nano_schema_history`

O framework cria e gerencia sozinho a tabela interna `nano_schema_history`:

```sql
CREATE TABLE IF NOT EXISTS nano_schema_history (
    version INTEGER PRIMARY KEY,
    description TEXT,
    script TEXT,
    installed_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    execution_time INTEGER,
    success INTEGER
);
```

Se um script falhar durante a migração, a transação daquele script sofre rollback e uma `MigrationException` é lançada na inicialização do servidor, impedindo que o banco fique em estado inconsistente.

[⬅ Voltar para o Índice](../index.md)
