package com.github.matheuscruzsouza.nanospring.storage;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BoundedInputStream extends FilterInputStream {
    private final AutoCloseable underlyingResource;
    private long remaining;

    public BoundedInputStream(InputStream in, long length) {
        this(in, length, null);
    }

    public BoundedInputStream(InputStream in, long length, AutoCloseable underlyingResource) {
        super(in);
        this.remaining = Math.max(0, length);
        this.underlyingResource = underlyingResource;
    }

    @Override
    public int read() throws IOException {
        if (remaining <= 0) {
            return -1;
        }
        int b = super.read();
        if (b != -1) {
            remaining--;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) {
            return -1;
        }
        int toRead = (int) Math.min(len, remaining);
        int read = super.read(b, off, toRead);
        if (read != -1) {
            remaining -= read;
        }
        return read;
    }

    @Override
    public int available() throws IOException {
        int avail = super.available();
        return (int) Math.min(avail, remaining);
    }

    @Override
    public long skip(long n) throws IOException {
        long toSkip = Math.min(n, remaining);
        long skipped = super.skip(toSkip);
        remaining -= skipped;
        return skipped;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if (underlyingResource != null) {
                try {
                    underlyingResource.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public long getRemaining() {
        return remaining;
    }
}
