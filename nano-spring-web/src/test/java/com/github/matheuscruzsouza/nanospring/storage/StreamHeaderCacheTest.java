package com.github.matheuscruzsouza.nanospring.storage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class StreamHeaderCacheTest {

    private File tempFile1;
    private File tempFile2;

    @Before
    public void setUp() throws IOException {
        tempFile1 = File.createTempFile("stream-cache-1-", ".dat");
        tempFile2 = File.createTempFile("stream-cache-2-", ".dat");

        try (FileOutputStream fos = new FileOutputStream(tempFile1)) {
            fos.write("ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes());
        }
        try (FileOutputStream fos = new FileOutputStream(tempFile2)) {
            fos.write("0123456789".getBytes());
        }
    }

    @After
    public void tearDown() {
        if (tempFile1 != null && tempFile1.exists()) {
            tempFile1.delete();
        }
        if (tempFile2 != null && tempFile2.exists()) {
            tempFile2.delete();
        }
    }

    @Test
    public void testGetOrLoadLoadsHeaderAndCaches() {
        StreamHeaderCache cache = new StreamHeaderCache(10, 1024);
        byte[] loaded1 = cache.getOrLoad(tempFile1);

        assertNotNull(loaded1);
        assertEquals(10, loaded1.length);
        assertArrayEquals("ABCDEFGHIJ".getBytes(), loaded1);

        // Second call should return identical cached byte array instance
        byte[] cached1 = cache.getOrLoad(tempFile1);
        assertSame(loaded1, cached1);
        assertEquals(10, cache.getCurrentMemoryUsage());
        assertEquals(1, cache.getCachedCount());
    }

    @Test
    public void testGetOrLoadSmallerFileThanHeaderSize() {
        StreamHeaderCache cache = new StreamHeaderCache(64, 1024);
        byte[] loaded2 = cache.getOrLoad(tempFile2);

        assertNotNull(loaded2);
        assertEquals(10, loaded2.length);
        assertArrayEquals("0123456789".getBytes(), loaded2);
        assertEquals(10, cache.getCurrentMemoryUsage());
    }

    @Test
    public void testEvictionPolicyWhenMaxMemoryExceeded() {
        // maxMemory = 15 bytes, each header is 10 bytes -> only 1 fits
        StreamHeaderCache cache = new StreamHeaderCache(10, 15);

        byte[] first = cache.getOrLoad(tempFile1);
        assertEquals(1, cache.getCachedCount());
        assertEquals(10, cache.getCurrentMemoryUsage());

        byte[] second = cache.getOrLoad(tempFile2);
        assertEquals(1, cache.getCachedCount());
        assertEquals(10, cache.getCurrentMemoryUsage());
        assertArrayEquals("0123456789".getBytes(), second);

        // Accessing tempFile1 again causes it to reload
        byte[] reloadedFirst = cache.getOrLoad(tempFile1);
        assertNotNull(reloadedFirst);
        assertArrayEquals("ABCDEFGHIJ".getBytes(), reloadedFirst);
    }

    @Test
    public void testGetOrLoadNonExistentFileReturnsNull() {
        StreamHeaderCache cache = new StreamHeaderCache(10, 1024);
        assertNull(cache.getOrLoad(null));
        assertNull(cache.getOrLoad(new File("non-existent-" + System.currentTimeMillis() + ".dat")));
    }
}
