package com.github.matheuscruzsouza.nanospring.storage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

public class FileResourceRegion implements ResourceRegion {

    private static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

    private final File file;
    private final String contentType;
    private FileInputStream currentInputStream;
    private InputStream bufferedStream;

    public FileResourceRegion(File file) {
        this(file, null);
    }

    public FileResourceRegion(File file, String contentType) {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null");
        }
        this.file = file;
        this.contentType = contentType != null ? contentType : "application/octet-stream";
    }

    public File getFile() {
        return file;
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
        currentInputStream = new FileInputStream(file);
        FileChannel channel = currentInputStream.getChannel();
        channel.position(position);
        bufferedStream = new BufferedInputStream(currentInputStream, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public InputStream openStream() throws IOException {
        if (currentInputStream == null) {
            currentInputStream = new FileInputStream(file);
            bufferedStream = new BufferedInputStream(currentInputStream, DEFAULT_BUFFER_SIZE);
        }
        return bufferedStream;
    }

    @Override
    public void close() throws IOException {
        try {
            if (bufferedStream != null) {
                bufferedStream.close();
            } else if (currentInputStream != null) {
                currentInputStream.close();
            }
        } finally {
            bufferedStream = null;
            currentInputStream = null;
        }
    }
}
