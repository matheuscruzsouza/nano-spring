package com.github.matheuscruzsouza.nanospring.storage;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.channels.FileChannel;

/**
 * High-performance {@link ResourceRegion} implementation that utilizes an optional in-memory header cache
 * and an enlarged sequential read buffer to minimize Android flash memory syscalls during media streaming.
 */
public class FastFileResourceRegion implements ResourceRegion {

    public static final int DEFAULT_BUFFER_SIZE = 512 * 1024; // 512 KB

    private final File file;
    private final String contentType;
    private final byte[] headerCache;
    private final int bufferSize;

    private FileInputStream currentInputStream;
    private InputStream activeStream;

    public FastFileResourceRegion(File file) {
        this(file, null, null, DEFAULT_BUFFER_SIZE);
    }

    public FastFileResourceRegion(File file, String contentType) {
        this(file, contentType, null, DEFAULT_BUFFER_SIZE);
    }

    public FastFileResourceRegion(File file, String contentType, byte[] headerCache) {
        this(file, contentType, headerCache, DEFAULT_BUFFER_SIZE);
    }

    public FastFileResourceRegion(File file, String contentType, byte[] headerCache, int bufferSize) {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null");
        }
        this.file = file;
        this.contentType = contentType != null ? contentType : "application/octet-stream";
        this.headerCache = headerCache;
        this.bufferSize = bufferSize > 0 ? bufferSize : DEFAULT_BUFFER_SIZE;
    }

    public File getFile() {
        return file;
    }

    public byte[] getHeaderCache() {
        return headerCache;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public String contentType() {
        return contentType;
    }

    @Override
    public void seek(long position) throws IOException {
        close();

        long cachedLen = (headerCache != null) ? headerCache.length : 0;

        if (position < cachedLen) {
            int offset = (int) position;
            int lengthFromCache = (int) (cachedLen - position);
            InputStream memStream = new ByteArrayInputStream(headerCache, offset, lengthFromCache);

            if (cachedLen < file.length()) {
                currentInputStream = new FileInputStream(file);
                FileChannel channel = currentInputStream.getChannel();
                channel.position(cachedLen);
                InputStream diskStream = new BufferedInputStream(currentInputStream, bufferSize);
                activeStream = new SequenceInputStream(memStream, diskStream);
            } else {
                activeStream = memStream;
            }
        } else {
            currentInputStream = new FileInputStream(file);
            FileChannel channel = currentInputStream.getChannel();
            channel.position(position);
            activeStream = new BufferedInputStream(currentInputStream, bufferSize);
        }
    }

    @Override
    public InputStream openStream() throws IOException {
        if (activeStream == null) {
            seek(0);
        }
        return activeStream;
    }

    @Override
    public void close() throws IOException {
        try {
            if (activeStream != null) {
                activeStream.close();
            } else if (currentInputStream != null) {
                currentInputStream.close();
            }
        } finally {
            activeStream = null;
            currentInputStream = null;
        }
    }
}
