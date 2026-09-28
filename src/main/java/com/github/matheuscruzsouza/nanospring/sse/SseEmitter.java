package com.github.matheuscruzsouza.nanospring.sse;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Emissor de Server-Sent Events (SSE) do Nano-Spring.
 * Permite transmissão contínua de dados em tempo real para navegadores e clientes HTTP.
 */
public class SseEmitter {

    private final PipedOutputStream pos;
    private final PipedInputStream pis;
    private final Gson gson = new Gson();
    private boolean completed = false;

    public SseEmitter() {
        this.pos = new PipedOutputStream();
        try {
            this.pis = new PipedInputStream(this.pos, 4096);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao inicializar pipes para SSE: " + e.getMessage(), e);
        }
    }

    public InputStream getInputStream() {
        return pis;
    }

    /**
     * Envia um evento de dados simples.
     * @param data Objeto ou String a ser enviado. Objetos serão serializados automaticamente para JSON.
     */
    public synchronized SseEmitter send(Object data) throws IOException {
        return send(null, data);
    }

    /**
     * Envia um evento SSE nomeado com payload de dados.
     * @param eventName Nome do evento (ex: "update", "log", "message").
     * @param data Conteúdo do evento.
     */
    public synchronized SseEmitter send(String eventName, Object data) throws IOException {
        if (completed) return this;

        StringBuilder sb = new StringBuilder();
        if (eventName != null && !eventName.trim().isEmpty()) {
            sb.append("event: ").append(eventName.trim()).append("\n");
        }

        String dataPayload = (data instanceof String) ? (String) data : gson.toJson(data);
        // Cada quebra de linha no payload deve ser precedida por 'data: ' conforme especificação SSE
        String[] lines = dataPayload.split("\n");
        for (String line : lines) {
            sb.append("data: ").append(line).append("\n");
        }
        sb.append("\n"); // Dupla quebra de linha indica término do bloco de evento

        pos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        pos.flush();
        return this;
    }

    /**
     * Envia um comentário keep-alive para manter a conexão ativa (evita timeout de proxies).
     */
    public synchronized SseEmitter sendComment(String comment) throws IOException {
        if (completed) return this;
        String line = ": " + comment + "\n\n";
        pos.write(line.getBytes(StandardCharsets.UTF_8));
        pos.flush();
        return this;
    }

    /**
     * Finaliza a transmissão de eventos e fecha a conexão com o cliente.
     */
    public synchronized void complete() {
        if (completed) return;
        completed = true;
        try {
            pos.close();
        } catch (IOException ignored) {}
    }

    /**
     * Finaliza a transmissão com erro.
     */
    public synchronized void completeWithError(Throwable ex) {
        complete();
    }
}
