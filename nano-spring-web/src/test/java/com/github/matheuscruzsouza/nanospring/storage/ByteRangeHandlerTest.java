package com.github.matheuscruzsouza.nanospring.storage;

import com.github.matheuscruzsouza.nanospring.http.HttpStatus;
import fi.iki.elonen.NanoHTTPD;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class ByteRangeHandlerTest {

    private File tempFile;
    private final String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("test-range-", ".dat");
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
        byte[] buf = new byte[256];
        int r;
        while ((r = is.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        return baos.toString();
    }

    @Test
    public void testFullContentWithoutRangeHeader() throws IOException {
        NanoHTTPD.IHTTPSession session = Mockito.mock(NanoHTTPD.IHTTPSession.class);
        when(session.getHeaders()).thenReturn(new HashMap<>());

        NanoHTTPD.Response response = ByteRangeHandler.serveFile(session, tempFile, "text/plain");

        assertEquals(NanoHTTPD.Response.Status.OK, response.getStatus());
        assertEquals("bytes", response.getHeader("Accept-Ranges"));
        assertEquals(String.valueOf(content.length()), response.getHeader("Content-Length"));
        assertEquals(content, readStream(response.getData()));
    }

    @Test
    public void testPartialContentRangeStartAndEnd() throws IOException {
        NanoHTTPD.IHTTPSession session = Mockito.mock(NanoHTTPD.IHTTPSession.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("range", "bytes=0-9");
        when(session.getHeaders()).thenReturn(headers);

        NanoHTTPD.Response response = ByteRangeHandler.serveFile(session, tempFile, "text/plain");

        assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatus());
        assertEquals("bytes", response.getHeader("Accept-Ranges"));
        assertEquals("10", response.getHeader("Content-Length"));
        assertEquals("bytes 0-9/" + content.length(), response.getHeader("Content-Range"));
        assertEquals("0123456789", readStream(response.getData()));
    }

    @Test
    public void testPartialContentRangeStartOnly() throws IOException {
        NanoHTTPD.IHTTPSession session = Mockito.mock(NanoHTTPD.IHTTPSession.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("range", "bytes=10-");
        when(session.getHeaders()).thenReturn(headers);

        NanoHTTPD.Response response = ByteRangeHandler.serveFile(session, tempFile, "text/plain");

        assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatus());
        long expectedLength = content.length() - 10;
        assertEquals(String.valueOf(expectedLength), response.getHeader("Content-Length"));
        assertEquals("bytes 10-" + (content.length() - 1) + "/" + content.length(), response.getHeader("Content-Range"));
        assertEquals(content.substring(10), readStream(response.getData()));
    }

    @Test
    public void testPartialContentSuffixRange() throws IOException {
        NanoHTTPD.IHTTPSession session = Mockito.mock(NanoHTTPD.IHTTPSession.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("range", "bytes=-6");
        when(session.getHeaders()).thenReturn(headers);

        NanoHTTPD.Response response = ByteRangeHandler.serveFile(session, tempFile, "text/plain");

        assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatus());
        assertEquals("6", response.getHeader("Content-Length"));
        String expectedSub = content.substring(content.length() - 6);
        assertEquals(expectedSub, readStream(response.getData()));
    }

    @Test
    public void testRangeNotSatisfiableWhenStartBeyondTotalLength() {
        NanoHTTPD.IHTTPSession session = Mockito.mock(NanoHTTPD.IHTTPSession.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("range", "bytes=1000-2000");
        when(session.getHeaders()).thenReturn(headers);

        NanoHTTPD.Response response = ByteRangeHandler.serveFile(session, tempFile, "text/plain");

        assertEquals(HttpStatus.RANGE_NOT_SATISFIABLE, response.getStatus());
        assertEquals("bytes */" + content.length(), response.getHeader("Content-Range"));
    }

    @Test
    public void testRangeNotSatisfiableWhenStartGreaterThanEnd() {
        NanoHTTPD.IHTTPSession session = Mockito.mock(NanoHTTPD.IHTTPSession.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("range", "bytes=20-10");
        when(session.getHeaders()).thenReturn(headers);

        NanoHTTPD.Response response = ByteRangeHandler.serveFile(session, tempFile, "text/plain");

        assertEquals(HttpStatus.RANGE_NOT_SATISFIABLE, response.getStatus());
        assertEquals("bytes */" + content.length(), response.getHeader("Content-Range"));
    }

    @Test
    public void testInvalidRangeFormat() {
        NanoHTTPD.IHTTPSession session = Mockito.mock(NanoHTTPD.IHTTPSession.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("range", "bytes=abc-xyz");
        when(session.getHeaders()).thenReturn(headers);

        NanoHTTPD.Response response = ByteRangeHandler.serveFile(session, tempFile, "text/plain");

        assertEquals(HttpStatus.RANGE_NOT_SATISFIABLE, response.getStatus());
        assertEquals("bytes */" + content.length(), response.getHeader("Content-Range"));
    }

    @Test
    public void testAutoDetectMediaMimeTypeWhenDefaultJsonIsPassed() throws IOException {
        File mp4File = File.createTempFile("video-test-", ".mp4");
        try {
            try (FileOutputStream fos = new FileOutputStream(mp4File)) {
                fos.write(content.getBytes());
            }
            NanoHTTPD.IHTTPSession session = Mockito.mock(NanoHTTPD.IHTTPSession.class);
            when(session.getHeaders()).thenReturn(new HashMap<>());

            NanoHTTPD.Response response = ByteRangeHandler.serveFile(session, mp4File, "application/json");
            assertEquals("video/mp4", response.getMimeType());
        } finally {
            mp4File.delete();
        }
    }
}
