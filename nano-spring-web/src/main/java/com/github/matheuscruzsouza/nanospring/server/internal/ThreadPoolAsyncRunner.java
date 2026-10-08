package com.github.matheuscruzsouza.nanospring.server.internal;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.github.matheuscruzsouza.nanospring.server.Environment;
import java.util.concurrent.atomic.AtomicInteger;

import fi.iki.elonen.NanoHTTPD;

/**
 * ThreadPool-based AsyncRunner for NanoHTTPD.
 * Prevents native thread OutOfMemoryError on Android by capping active worker threads
 * and request queue size.
 */
public class ThreadPoolAsyncRunner implements NanoHTTPD.AsyncRunner {

    private static final String TAG = "ThreadPoolAsyncRunner";
    public static final int DEFAULT_CORE_POOL_SIZE = 4;
    public static final int DEFAULT_MAX_POOL_SIZE = 16;
    public static final int DEFAULT_QUEUE_CAPACITY = 100;
    public static final long DEFAULT_KEEP_ALIVE_SECONDS = 60L;

    private final ThreadPoolExecutor executor;
    private final List<NanoHTTPD.ClientHandler> running = Collections.synchronizedList(new ArrayList<>());
    private final int corePoolSize;
    private final int maxPoolSize;
    private final int queueCapacity;
    private final long keepAliveSeconds;

    public ThreadPoolAsyncRunner() {
        this(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_POOL_SIZE, DEFAULT_QUEUE_CAPACITY, DEFAULT_KEEP_ALIVE_SECONDS);
    }

    public ThreadPoolAsyncRunner(int corePoolSize, int maxPoolSize, int queueCapacity, long keepAliveSeconds) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueCapacity = queueCapacity;
        this.keepAliveSeconds = keepAliveSeconds;

        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueCapacity);
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "nano-spring-worker-" + threadIndex.getAndIncrement());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };

        this.executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                queue,
                threadFactory
        );
    }

    public static ThreadPoolAsyncRunner fromEnvironment() {
        boolean isLowMemory = Environment.hasActiveProfile("low-memory");
        
        int defCore = isLowMemory ? 2 : DEFAULT_CORE_POOL_SIZE;
        int defMax = isLowMemory ? 4 : DEFAULT_MAX_POOL_SIZE;
        int defQueue = isLowMemory ? 50 : DEFAULT_QUEUE_CAPACITY;
        
        int core = parsePositiveInt(Environment.getProperty("nano.server.threads.core", String.valueOf(defCore)), defCore);
        int max = parsePositiveInt(Environment.getProperty("nano.server.threads.max", String.valueOf(defMax)), defMax);
        int queue = parsePositiveInt(Environment.getProperty("nano.server.threads.queue-capacity", String.valueOf(defQueue)), defQueue);
        long keepAlive = parsePositiveLong(Environment.getProperty("nano.server.threads.keep-alive", String.valueOf(DEFAULT_KEEP_ALIVE_SECONDS)), DEFAULT_KEEP_ALIVE_SECONDS);

        if (max < core) {
            max = core;
        }

        return new ThreadPoolAsyncRunner(core, max, queue, keepAlive);
    }
    private static int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long parsePositiveLong(String value, long fallback) {
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void exec(NanoHTTPD.ClientHandler clientHandler) {
        running.add(clientHandler);
        try {
            executor.execute(clientHandler);
        } catch (RejectedExecutionException e) {
            running.remove(clientHandler);
            Log.w(TAG, "Request pool capacity saturated. Rejecting incoming connection.", e);
            try {
                clientHandler.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void closed(NanoHTTPD.ClientHandler clientHandler) {
        running.remove(clientHandler);
    }

    @Override
    public void closeAll() {
        for (NanoHTTPD.ClientHandler clientHandler : new ArrayList<>(running)) {
            try {
                clientHandler.close();
            } catch (Exception ignored) {
            }
        }
        running.clear();
        executor.shutdown();
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public long getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public int getQueueSize() {
        return executor.getQueue().size();
    }

    public List<NanoHTTPD.ClientHandler> getRunning() {
        return Collections.unmodifiableList(new ArrayList<>(running));
    }
}
