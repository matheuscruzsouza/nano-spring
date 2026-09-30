# 📁 Upload e Streaming de Arquivos

O **Nano-Spring** simplifica o envio e recebimento de arquivos no Android, com proteção de memória (*streaming chunked*) para evitar estourar a memória RAM (*OutOfMemoryError*) em dispositivos móveis.

---

## 📤 Upload de Arquivos (`multipart/form-data`)

Para receber arquivos de formulários web ou de chamadas de API, use a anotação `@UploadedFile`:

```java
package com.exemplo.meuapp.controller;

import com.github.matheuscruzsouza.nanospring.annotation.PostMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.annotation.UploadedFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@RestController("/api/files")
public class FileUploadController {

    @PostMethod("/upload")
    public String uploadArquivo(@UploadedFile("foto") File tempFile) {
        if (tempFile == null || !tempFile.exists()) {
            return "Nenhum arquivo enviado.";
        }

        // O Nano-Spring salva temporariamente na pasta de cache do Android
        long bytes = tempFile.length();

        // Mova para armazenamento definitivo do aplicativo se desejar:
        File destino = new File(tempFile.getParentFile(), "upload_salvo_" + System.currentTimeMillis() + ".dat");
        boolean sucesso = tempFile.renameTo(destino);

        return "Arquivo recebido com sucesso (" + bytes + " bytes)! Salvo: " + sucesso;
    }
}
```

### Exemplo de Envio via HTML:
```html
<form action="/api/files/upload" method="post" enctype="multipart/form-data">
    <input type="file" name="foto" required />
    <button type="submit">Enviar Arquivo</button>
</form>
```

---

## 📥 Download e Streaming de Arquivos (`File`)

Quando seu método no `@RestController` retorna uma instância de `java.io.File`, o Nano-Spring ativa transmissão em blocos (*Chunked Transfer Encoding*):

```java
package com.exemplo.meuapp.controller;

import android.content.Context;
import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PathVariable;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.http.HttpStatus;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;
import java.io.File;

@RestController("/api/files")
public class FileDownloadController {

    @Autowired
    private Context context;

    @GetMethod(value = "/download/:nome", mimeType = "application/octet-stream")
    public Object downloadArquivo(@PathVariable("nome") String nome) {
        File arquivo = new File(context.getFilesDir(), nome);

        if (!arquivo.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Arquivo não encontrado");
        }

        // Retornando diretamente o File, o Nano-Spring faz o streaming em blocos!
        return arquivo;
    }
}
```

---

## ⚡ Vantagens do Streaming no Android

* **Baixo consumo de RAM:** Arquivos de 500 MB ou mais são transmitidos diretamente do disco para o socket TCP em buffers pequenos, sem carregar o arquivo inteiro na memória do celular.
* **Header de Download:** Ao retornar `java.io.File`, o cabeçalho `Content-Disposition: attachment; filename="<nome>"` é injetado automaticamente pelo framework.

[⬅ Voltar para o Índice](../index.md)
