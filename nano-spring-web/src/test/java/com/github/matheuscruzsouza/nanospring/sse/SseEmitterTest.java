package com.github.matheuscruzsouza.nanospring.sse;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SseEmitterTest {

    @Test
    public void testSendDataAndFormat() throws Exception {
        SseEmitter emitter = new SseEmitter();
        InputStream is = emitter.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        emitter.send("Olá Mundo");

        assertEquals("data: Olá Mundo", reader.readLine());
        assertEquals("", reader.readLine()); // linha em branco separadora
    }

    @Test
    public void testSendNamedEventWithJsonPayload() throws Exception {
        SseEmitter emitter = new SseEmitter();
        InputStream is = emitter.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        TestPayload payload = new TestPayload("item1", 42);
        emitter.send("custom-event", payload);

        assertEquals("event: custom-event", reader.readLine());
        assertTrue(reader.readLine().startsWith("data: {\"name\":\"item1\",\"count\":42"));
        assertEquals("", reader.readLine());
    }

    @Test
    public void testSendComment() throws Exception {
        SseEmitter emitter = new SseEmitter();
        InputStream is = emitter.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        emitter.sendComment("ping");

        assertEquals(": ping", reader.readLine());
        assertEquals("", reader.readLine());
    }

    @Test
    public void testOnCompletionCallback() {
        SseEmitter emitter = new SseEmitter();
        final AtomicBoolean completed = new AtomicBoolean(false);

        emitter.onCompletion(new SseEmitter.CompletionCallback() {
            @Override
            public void onCompletion() {
                completed.set(true);
            }
        });

        assertFalse(emitter.isCompleted());
        emitter.complete();
        assertTrue(emitter.isCompleted());
        assertTrue(completed.get());
    }

    @Test
    public void testOnErrorCallback() {
        SseEmitter emitter = new SseEmitter();
        final AtomicReference<Throwable> caughtError = new AtomicReference<>();

        emitter.onError(new SseEmitter.ErrorCallback() {
            @Override
            public void onError(Throwable throwable) {
                caughtError.set(throwable);
            }
        });

        RuntimeException ex = new RuntimeException("Falha de teste");
        emitter.completeWithError(ex);

        assertTrue(emitter.isCompleted());
        assertNotNull(caughtError.get());
        assertEquals("Falha de teste", caughtError.get().getMessage());
    }

    @Test
    public void testTimeoutTriggersCallbackAndCompletes() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean timedOut = new AtomicBoolean(false);

        SseEmitter emitter = new SseEmitter(100L); // 100ms timeout
        emitter.onTimeout(new SseEmitter.TimeoutCallback() {
            @Override
            public void onTimeout() {
                timedOut.set(true);
                latch.countDown();
            }
        });

        boolean called = latch.await(1, TimeUnit.SECONDS);
        assertTrue("Timeout callback should be called within 1 second", called);
        assertTrue(timedOut.get());
        assertTrue(emitter.isCompleted());
    }

    private static class TestPayload {
        private final String name;
        private final int count;

        public TestPayload(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }
}
