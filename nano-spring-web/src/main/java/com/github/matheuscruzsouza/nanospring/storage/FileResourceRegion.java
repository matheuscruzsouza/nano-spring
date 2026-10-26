package com.github.matheuscruzsouza.nanospring.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

public class FileResourceRegion implements ResourceRegion {

    private final File file;
    private final String contentType;
    private FileInputStream currentInputStream;

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
    }

    @Override
    public InputStream openStream() throws IOException {
        if (currentInputStream == null) {
            currentInputStream = new FileInputStream(file);
        }
        return currentInputStream;
    }

    @Override
    public void close() throws IOException {
        if (currentInputStream != null) {
            try {
                currentInputStream.close();
            } finally {
                currentInputStream = null;
            }
        }
    }
}
