package com.github.matheuscruzsouza.nanospring.server;

import org.junit.After;
import org.junit.Test;

import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import fi.iki.elonen.NanoHTTPD;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ThreadPoolAsyncRunnerTest {

    private ThreadPoolAsyncRunner runner;

    @After
    public void tearDown() {
        if (runner != null) {
            runner.closeAll();
        }
    }

    @Test
    public void testDefaultConfiguration() {
        runner = new ThreadPoolAsyncRunner();
        assertEquals(4, runner.getCorePoolSize());
        assertEquals(16, runner.getMaxPoolSize());
        assertEquals(100, runner.getQueueCapacity());
        assertEquals(60L, runner.getKeepAliveSeconds());
        assertNotNull(runner.getExecutor());
    }

    @Test
    public void testCustomConfiguration() {
        runner = new ThreadPoolAsyncRunner(2, 8, 50, 30L);
        assertEquals(2, runner.getCorePoolSize());
        assertEquals(8, runner.getMaxPoolSize());
        assertEquals(50, runner.getQueueCapacity());
        assertEquals(30L, runner.getKeepAliveSeconds());
    }

    @Test
    public void testExecRunsClientHandler() throws Exception {
        runner = new ThreadPoolAsyncRunner(2, 4, 10, 10L);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);

        DummyServer dummyServer = new DummyServer();
        NanoHTTPD.ClientHandler handler = dummyServer.createDummyClientHandler(() -> {
            executed.set(true);
            latch.countDown();
        });

        runner.exec(handler);
        boolean completed = latch.await(2, TimeUnit.SECONDS);

        assertTrue("Task should have executed within timeout", completed);
        assertTrue("Handler should have set executed to true", executed.get());
    }

    @Test
    public void testCloseAllShutsDownExecutor() {
        runner = new ThreadPoolAsyncRunner(2, 4, 10, 10L);
        runner.closeAll();
        assertTrue("Executor should be shutdown after closeAll", runner.getExecutor().isShutdown());
        assertEquals(0, runner.getRunning().size());
    }

    private static class DummyServer extends NanoHTTPD {
        public DummyServer() {
            super(0);
        }

        public ClientHandler createDummyClientHandler(final Runnable task) {
            return new ClientHandler(new InputStream() {
                @Override
                public int read() {
                    return -1;
                }
            }, new Socket()) {
                @Override
                public void run() {
                    task.run();
                }

                @Override
                public void close() {
                }
            };
        }
    }
}
