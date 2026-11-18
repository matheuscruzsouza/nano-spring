package com.github.matheuscruzsouza.nanospring.media.streaming;

import com.github.matheuscruzsouza.nanospring.media.storage.FileEntry;
import com.github.matheuscruzsouza.nanospring.media.storage.StorageProvider;
import com.github.matheuscruzsouza.nanospring.storage.ResourceRegion;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link ResourceRegion} implementation that streams chunks directly from a {@link StorageProvider}
 * with offset-based seeking for HTTP 206 Partial Content.
 */
public class MediaResourceRegion implements ResourceRegion {

    private final StorageProvider storageProvider;
    private final String path;
    private final long contentLength;
    private final String contentType;

    private long position = 0L;
    private InputStream currentStream;

    public MediaResourceRegion(StorageProvider storageProvider, String path, long contentLength, String contentType) {
        if (storageProvider == null) {
            throw new IllegalArgumentException("StorageProvider must not be null");
        }
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        this.storageProvider = storageProvider;
        this.path = path;
        this.contentLength = contentLength;
        this.contentType = contentType != null ? contentType : "application/octet-stream";
    }

    public MediaResourceRegion(StorageProvider storageProvider, FileEntry entry, String contentType) {
        this(storageProvider, entry.getPath(), entry.getSize(), contentType);
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public String contentType() {
        return contentType;
    }

    @Override
    public void seek(long position) throws IOException {
        if (position < 0) {
            throw new IllegalArgumentException("Seek position must be non-negative: " + position);
        }
        this.position = position;
        closeCurrentStream();
    }

    @Override
    public InputStream openStream() throws IOException {
        closeCurrentStream();
        currentStream = storageProvider.openStream(path, position);
        return currentStream;
    }

    @Override
    public void close() throws IOException {
        closeCurrentStream();
    }

    private void closeCurrentStream() throws IOException {
        if (currentStream != null) {
            try {
                currentStream.close();
            } finally {
                currentStream = null;
            }
        }
    }
}
