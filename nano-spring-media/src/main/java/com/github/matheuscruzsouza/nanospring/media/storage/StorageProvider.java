package com.github.matheuscruzsouza.nanospring.media.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Agnostic storage abstraction for local and network (e.g., SMB) file systems.
 */
public interface StorageProvider {

    boolean exists(String path) throws IOException;

    FileEntry stat(String path) throws IOException;

    List<FileEntry> list(String path) throws IOException;

    /**
     * Opens an {@link InputStream} positioned at the given byte {@code offset}.
     * Callers are responsible for closing the returned stream.
     */
    InputStream openStream(String path, long offset) throws IOException;
}
