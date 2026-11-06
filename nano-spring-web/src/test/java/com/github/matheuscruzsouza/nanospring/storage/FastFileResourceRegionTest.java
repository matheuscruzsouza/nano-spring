package com.github.matheuscruzsouza.nanospring.storage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FastFileResourceRegionTest {

    private File tempFile;
    private final String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("test-fast-file-", ".dat");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(content.getBytes());
        }
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[64];
        int r;
        while ((r = is.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        return baos.toString();
    }

    @Test
    public void testFullReadWithoutHeaderCache() throws IOException {
        try (FastFileResourceRegion region = new FastFileResourceRegion(tempFile, "text/plain", null, 128)) {
            assertEquals(content.length(), region.contentLength());
            assertEquals("text/plain", region.contentType());
            assertEquals(content, readStream(region.openStream()));
        }
    }

    @Test
    public void testReadWithHeaderCacheBridgedToDisk() throws IOException {
        // Cache first 10 bytes: "0123456789"
        byte[] header = "0123456789".getBytes();
        try (FastFileResourceRegion region = new FastFileResourceRegion(tempFile, "text/plain", header, 128)) {
            region.seek(0);
            assertEquals(content, readStream(region.openStream()));
        }
    }

    @Test
    public void testSeekWithinHeaderCacheOffset() throws IOException {
        // Cache first 10 bytes: "0123456789"
        byte[] header = "0123456789".getBytes();
        try (FastFileResourceRegion region = new FastFileResourceRegion(tempFile, "text/plain", header, 128)) {
            region.seek(5); // starts at '5' in RAM cache, then transitions smoothly into disk at index 10
            assertEquals(content.substring(5), readStream(region.openStream()));
        }
    }

    @Test
    public void testSeekBeyondHeaderCacheOffset() throws IOException {
        byte[] header = "0123456789".getBytes();
        try (FastFileResourceRegion region = new FastFileResourceRegion(tempFile, "text/plain", header, 128)) {
            region.seek(15); // completely on disk
            assertEquals(content.substring(15), readStream(region.openStream()));
        }
    }

    @Test
    public void testFileResourceRegionInheritance() throws IOException {
        byte[] header = "0123456789".getBytes();
        try (FileResourceRegion region = new FileResourceRegion(tempFile, "audio/mpeg", header, 128)) {
            assertEquals("audio/mpeg", region.contentType());
            region.seek(2);
            assertEquals(content.substring(2), readStream(region.openStream()));
        }
    }
}
