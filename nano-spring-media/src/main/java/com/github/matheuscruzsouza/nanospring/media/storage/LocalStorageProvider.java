package com.github.matheuscruzsouza.nanospring.media.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link StorageProvider} implementation backed by the local filesystem via {@link File}.
 * Compatible with Android API 25+ (avoids {@code java.nio.file} which requires API 26).
 */
public class LocalStorageProvider implements StorageProvider {

    private final File rootDir;
    private final String canonicalRootPath;

    public LocalStorageProvider() {
        this.rootDir = null;
        this.canonicalRootPath = null;
    }

    public LocalStorageProvider(File rootDir) {
        if (rootDir != null) {
            try {
                this.rootDir = rootDir.getCanonicalFile();
                this.canonicalRootPath = this.rootDir.getPath();
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid root directory: " + rootDir, e);
            }
        } else {
            this.rootDir = null;
            this.canonicalRootPath = null;
        }
    }

    @Override
    public boolean exists(String path) throws IOException {
        File resolved = resolveFile(path);
        return resolved.exists();
    }

    @Override
    public FileEntry stat(String path) throws IOException {
        File resolved = resolveFile(path);
        if (!resolved.exists()) {
            throw new FileNotFoundException("File not found: " + path);
        }
        boolean isDir = resolved.isDirectory();
        long size = isDir ? 0L : resolved.length();
        return new FileEntry(
                resolved.getName(),
                toRelativeOrAbsolutePath(resolved),
                size,
                isDir,
                resolved.lastModified()
        );
    }

    @Override
    public List<FileEntry> list(String path) throws IOException {
        File resolved = resolveFile(path);
        if (!resolved.exists() || !resolved.isDirectory()) {
            return Collections.emptyList();
        }
        File[] children = resolved.listFiles();
        if (children == null || children.length == 0) {
            return Collections.emptyList();
        }
        List<FileEntry> entries = new ArrayList<>(children.length);
        for (File child : children) {
            boolean isDir = child.isDirectory();
            long size = isDir ? 0L : child.length();
            entries.add(new FileEntry(
                    child.getName(),
                    toRelativeOrAbsolutePath(child),
                    size,
                    isDir,
                    child.lastModified()
            ));
        }
        return entries;
    }

    @Override
    public InputStream openStream(String path, long offset) throws IOException {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative: " + offset);
        }
        File resolved = resolveFile(path);
        if (!resolved.exists() || resolved.isDirectory()) {
            throw new FileNotFoundException("File not found or is a directory: " + path);
        }
        FileInputStream fis = new FileInputStream(resolved);
        try {
            FileChannel channel = fis.getChannel();
            if (offset > 0) {
                channel.position(offset);
            }
            return Channels.newInputStream(channel);
        } catch (IOException | RuntimeException e) {
            fis.close();
            throw e;
        }
    }

    private File resolveFile(String path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        if (rootDir == null) {
            return new File(path).getCanonicalFile();
        }
        String sanitized = path.startsWith("/") ? path.substring(1) : path;
        File resolved = new File(rootDir, sanitized).getCanonicalFile();
        String resolvedPath = resolved.getPath();
        if (!resolvedPath.equals(canonicalRootPath)
                && !resolvedPath.startsWith(canonicalRootPath + File.separator)) {
            throw new IOException("Path escapes root directory: " + path);
        }
        return resolved;
    }

    private String toRelativeOrAbsolutePath(File resolved) {
        if (canonicalRootPath != null) {
            String fullPath = resolved.getPath();
            if (fullPath.equals(canonicalRootPath)) {
                return "";
            }
            String prefix = canonicalRootPath + File.separator;
            if (fullPath.startsWith(prefix)) {
                return fullPath.substring(prefix.length());
            }
        }
        return resolved.getPath();
    }
}
