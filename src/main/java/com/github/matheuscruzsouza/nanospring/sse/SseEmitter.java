package com.github.matheuscruzsouza.nanospring.sse;

import com.github.matheuscruzsouza.nanospring.server.Environment;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Emissor de Server-Sent Events (SSE) do Nano-Spring.
 * Permite transmissão contínua de dados em tempo real para navegadores e clientes HTTP.
 * Inclui gerenciamento de ciclo de vida e heartbeat keep-alive automático para detecção
 * precoce de desconexão e prevenção de starvation de threads no servidor.
 */
public class SseEmitter {

    public interface CompletionCallback {
        void onCompletion();
    }

    public interface ErrorCallback {
        void onError(Throwable throwable);
    }

    public interface TimeoutCallback {
        void onTimeout();
    }

    private static final String DEFAULT_HEARTBEAT_MS_STR = "15000";

    // Scheduler compartilhado leve (daemon threads) criado sob demanda para heartbeats e timeouts
    private static volatile ScheduledExecutorService scheduler;

    private static ScheduledExecutorService getScheduler() {
        if (scheduler == null) {
            synchronized (SseEmitter.class) {
                if (scheduler == null) {
                    scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "nano-spring-sse-scheduler");
                            t.setDaemon(true);
                            t.setPriority(Thread.NORM_PRIORITY);
                            return t;
                        }
                    });
                }
            }
        }
        return scheduler;
    }

    private final PipedOutputStream pos;
    private final PipedInputStream pis;
    private final Gson gson = new Gson();
    private final AtomicBoolean completed = new AtomicBoolean(false);

    private final List<CompletionCallback> completionCallbacks = new CopyOnWriteArrayList<>();
    private final List<ErrorCallback> errorCallbacks = new CopyOnWriteArrayList<>();
    private final List<TimeoutCallback> timeoutCallbacks = new CopyOnWriteArrayList<>();

    private ScheduledFuture<?> heartbeatFuture;
    private ScheduledFuture<?> timeoutFuture;

    public SseEmitter() {
        this(resolveDefaultTimeout());
    }

    public SseEmitter(Long timeoutMs) {
        this.pos = new PipedOutputStream();
        try {
            this.pis = new PipedInputStream(this.pos, 4096);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao inicializar pipes para SSE: " + e.getMessage(), e);
        }

        setupHeartbeat();
        setupTimeout(timeoutMs);
    }

    private static Long resolveDefaultTimeout() {
        try {
            String timeoutProp = Environment.getProperty("nano.sse.timeout-ms");
            if (timeoutProp != null && !timeoutProp.trim().isEmpty()) {
                long val = Long.parseLong(timeoutProp.trim());
                return val > 0 ? val : null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void setupHeartbeat() {
        long heartbeatInterval = 15000L;
        try {
            String prop = Environment.getProperty("nano.sse.heartbeat-interval-ms", DEFAULT_HEARTBEAT_MS_STR);
            if (prop != null) {
                heartbeatInterval = Long.parseLong(prop.trim());
            }
        } catch (Exception ignored) {}

        if (heartbeatInterval > 0) {
            final long interval = heartbeatInterval;
            this.heartbeatFuture = getScheduler().scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    if (isCompleted()) {
                        cancelTasks();
                        return;
                    }
                    try {
                        sendComment("keep-alive");
                    } catch (Exception ex) {
                        completeWithError(ex);
                    }
                }
            }, interval, interval, TimeUnit.MILLISECONDS);
        }
    }

    private void setupTimeout(Long timeoutMs) {
        if (timeoutMs != null && timeoutMs > 0) {
            this.timeoutFuture = getScheduler().schedule(new Runnable() {
                @Override
                public void run() {
                    if (!isCompleted()) {
                        for (TimeoutCallback callback : timeoutCallbacks) {
                            try {
                                callback.onTimeout();
                            } catch (Throwable ignored) {}
                        }
                        complete();
                    }
                }
            }, timeoutMs, TimeUnit.MILLISECONDS);
        }
    }

    public InputStream getInputStream() {
        return pis;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public SseEmitter onCompletion(CompletionCallback callback) {
        if (callback != null) {
            if (isCompleted()) {
                callback.onCompletion();
            } else {
                completionCallbacks.add(callback);
            }
        }
        return this;
    }

    public SseEmitter onError(ErrorCallback callback) {
        if (callback != null) {
            errorCallbacks.add(callback);
        }
        return this;
    }

    public SseEmitter onTimeout(TimeoutCallback callback) {
        if (callback != null) {
            timeoutCallbacks.add(callback);
        }
        return this;
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
        if (completed.get()) return this;

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

        try {
            pos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            pos.flush();
        } catch (IOException e) {
            completeWithError(e);
            throw e;
        }
        return this;
    }

    /**
     * Envia um comentário keep-alive para manter a conexão ativa (evita timeout de proxies).
     */
    public synchronized SseEmitter sendComment(String comment) throws IOException {
        if (completed.get()) return this;
        String line = ": " + comment + "\n\n";
        try {
            pos.write(line.getBytes(StandardCharsets.UTF_8));
            pos.flush();
        } catch (IOException e) {
            completeWithError(e);
            throw e;
        }
        return this;
    }

    /**
     * Finaliza a transmissão de eventos e fecha a conexão com o cliente.
     */
    public synchronized void complete() {
        if (!completed.compareAndSet(false, true)) {
            return;
        }
        cancelTasks();
        try {
            pos.close();
        } catch (IOException ignored) {}

        for (CompletionCallback callback : completionCallbacks) {
            try {
                callback.onCompletion();
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Finaliza a transmissão com erro.
     */
    public synchronized void completeWithError(Throwable ex) {
        if (completed.get()) {
            return;
        }
        for (ErrorCallback callback : errorCallbacks) {
            try {
                callback.onError(ex);
            } catch (Throwable ignored) {}
        }
        complete();
    }

    private void cancelTasks() {
        if (heartbeatFuture != null && !heartbeatFuture.isDone()) {
            heartbeatFuture.cancel(false);
        }
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(false);
        }
    }
}
