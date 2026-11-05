package com.github.matheuscruzsouza.nanospring.storage;

import java.io.File;

/**
 * Standard {@link ResourceRegion} backed by a {@link File}.
 * Inherits high-throughput buffered disk and cache-aware streaming from {@link FastFileResourceRegion}.
 */
public class FileResourceRegion extends FastFileResourceRegion {

    public FileResourceRegion(File file) {
        super(file, null, null, DEFAULT_BUFFER_SIZE);
    }

    public FileResourceRegion(File file, String contentType) {
        super(file, contentType, null, DEFAULT_BUFFER_SIZE);
    }

    public FileResourceRegion(File file, String contentType, byte[] headerCache) {
        super(file, contentType, headerCache, DEFAULT_BUFFER_SIZE);
    }

    public FileResourceRegion(File file, String contentType, byte[] headerCache, int bufferSize) {
        super(file, contentType, headerCache, bufferSize);
    }
}
