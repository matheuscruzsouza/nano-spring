# Persistência com SQLite (Nano-Spring)

O Nano-Spring integra-se diretamente com a API nativa de `SQLiteDatabase` do Android.
Não há Hibernate ou JPA completo. Use consultas SQL brutas (`rawQuery`) ou os métodos do `SQLiteDatabase`.

## Repositórios
Use a anotação `@Repository` do pacote `com.github.matheuscruzsouza.nanospring.annotation.Repository`.

### Exemplo de Repositório
```java
import com.github.matheuscruzsouza.nanospring.annotation.Repository;
import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import java.util.List;
import java.util.ArrayList;

@Repository
public class UserRepository {

    @Autowired
    private SQLiteDatabase db;

    public List<String> listUsers() {
        List<String> users = new ArrayList<>();

        // Usar try-with-resources para garantir que o cursor feche
        try (Cursor cursor = db.rawQuery("SELECT nome FROM users", null)) {
            while (cursor.moveToNext()) {
                users.add(cursor.getString(0));
            }
        }

        return users;
    }
}
```

## Migrations (Migrações de Banco de Dados)
Os arquivos de migração SQL devem ser colocados em: `assets/db/migration/` (na pasta `app/src/main/assets/db/migration/`).

Nomeie os arquivos usando a convenção de versão:
- `V1__create_users_table.sql`
- `V2__add_status_column.sql`

O Nano-Spring executará essas migrações automaticamente no startup.

Para ativar o modo WAL do SQLite (melhora concorrência local), use no `application.properties`:
`nano.datasource.wal.enabled=true`
