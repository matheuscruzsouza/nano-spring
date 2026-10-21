package com.github.matheuscruzsouza.nanospring.storage;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BoundedInputStreamTest {

    @Test
    public void testReadBoundedBytes() throws IOException {
        byte[] data = "Hello, World! 0123456789".getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        BoundedInputStream bounded = new BoundedInputStream(bais, 5);

        byte[] buffer = new byte[10];
        int read = bounded.read(buffer, 0, buffer.length);

        assertEquals(5, read);
        assertEquals("Hello", new String(buffer, 0, read));
        assertEquals(-1, bounded.read());
        bounded.close();
    }

    @Test
    public void testReadSingleByteBounded() throws IOException {
        byte[] data = new byte[]{10, 20, 30};
        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        BoundedInputStream bounded = new BoundedInputStream(bais, 2);

        assertEquals(10, bounded.read());
        assertEquals(20, bounded.read());
        assertEquals(-1, bounded.read());
        bounded.close();
    }

    @Test
    public void testSkipAndAvailable() throws IOException {
        byte[] data = "1234567890".getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        BoundedInputStream bounded = new BoundedInputStream(bais, 6);
        assertEquals(6, bounded.available());

        long skipped = bounded.skip(2);
        assertEquals(2, skipped);
        assertEquals(4, bounded.available());

        byte[] buf = new byte[4];
        int read = bounded.read(buf);
        assertEquals(4, read);
        assertArrayEquals("3456".getBytes(), buf);
        assertEquals(-1, bounded.read());
        bounded.close();
    }

    @Test
    public void testAutoCloseUnderlyingResource() throws IOException {
        final AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable resource = () -> closed.set(true);

        ByteArrayInputStream bais = new ByteArrayInputStream("test".getBytes());
        BoundedInputStream bounded = new BoundedInputStream(bais, 4, resource);

        bounded.close();
        assertTrue(closed.get());
    }
}
