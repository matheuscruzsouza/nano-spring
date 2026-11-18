package com.github.matheuscruzsouza.nanospring.media.storage;

import java.util.Objects;

/**
 * Basic metadata for a file or directory in a {@link StorageProvider}.
 */
public class FileEntry {

    private final String name;
    private final String path;
    private final long size;
    private final boolean directory;
    private final long lastModified;

    public FileEntry(String name, String path, long size, boolean directory, long lastModified) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.directory = directory;
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public boolean isDirectory() {
        return directory;
    }

    public long getLastModified() {
        return lastModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileEntry)) {
            return false;
        }
        FileEntry fileEntry = (FileEntry) o;
        return size == fileEntry.size
                && directory == fileEntry.directory
                && lastModified == fileEntry.lastModified
                && Objects.equals(name, fileEntry.name)
                && Objects.equals(path, fileEntry.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path, size, directory, lastModified);
    }

    @Override
    public String toString() {
        return "FileEntry{"
                + "name='" + name + '\''
                + ", path='" + path + '\''
                + ", size=" + size
                + ", directory=" + directory
                + ", lastModified=" + lastModified
                + '}';
    }
}
