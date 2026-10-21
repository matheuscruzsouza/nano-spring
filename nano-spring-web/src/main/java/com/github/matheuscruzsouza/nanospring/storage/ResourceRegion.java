package com.github.matheuscruzsouza.nanospring.storage;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public interface ResourceRegion extends Closeable {

    long contentLength();

    String contentType();

    void seek(long position) throws IOException;

    InputStream openStream() throws IOException;

    @Override
    void close() throws IOException;
}
