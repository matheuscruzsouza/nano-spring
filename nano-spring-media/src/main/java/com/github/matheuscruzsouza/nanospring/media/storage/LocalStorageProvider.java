package com.github.matheuscruzsouza.nanospring.media.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link StorageProvider} implementation backed by the local filesystem via {@code java.nio.file}.
 */
public class LocalStorageProvider implements StorageProvider {

    private final Path rootPath;

    public LocalStorageProvider() {
        this.rootPath = null;
    }

    public LocalStorageProvider(File rootDir) {
        this.rootPath = rootDir != null ? rootDir.toPath().toAbsolutePath().normalize() : null;
    }

    public LocalStorageProvider(Path rootPath) {
        this.rootPath = rootPath != null ? rootPath.toAbsolutePath().normalize() : null;
    }

    @Override
    public boolean exists(String path) throws IOException {
        Path resolved = resolvePath(path);
        return Files.exists(resolved);
    }

    @Override
    public FileEntry stat(String path) throws IOException {
        Path resolved = resolvePath(path);
        if (!Files.exists(resolved)) {
            throw new FileNotFoundException("File not found: " + path);
        }
        File file = resolved.toFile();
        boolean isDir = file.isDirectory();
        long size = isDir ? 0L : file.length();
        return new FileEntry(
                file.getName(),
                toRelativeOrAbsolutePath(resolved),
                size,
                isDir,
                file.lastModified()
        );
    }

    @Override
    public List<FileEntry> list(String path) throws IOException {
        Path resolved = resolvePath(path);
        if (!Files.exists(resolved) || !Files.isDirectory(resolved)) {
            return Collections.emptyList();
        }
        File[] children = resolved.toFile().listFiles();
        if (children == null || children.length == 0) {
            return Collections.emptyList();
        }
        List<FileEntry> entries = new ArrayList<>(children.length);
        for (File child : children) {
            boolean isDir = child.isDirectory();
            long size = isDir ? 0L : child.length();
            entries.add(new FileEntry(
                    child.getName(),
                    toRelativeOrAbsolutePath(child.toPath()),
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
        Path resolved = resolvePath(path);
        if (!Files.exists(resolved) || Files.isDirectory(resolved)) {
            throw new FileNotFoundException("File not found or is a directory: " + path);
        }
        FileChannel channel = FileChannel.open(resolved, StandardOpenOption.READ);
        try {
            if (offset > 0) {
                channel.position(offset);
            }
            return Channels.newInputStream(channel);
        } catch (IOException | RuntimeException e) {
            channel.close();
            throw e;
        }
    }

    private Path resolvePath(String path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        if (rootPath == null) {
            return Paths.get(path).toAbsolutePath().normalize();
        }
        String sanitized = path.startsWith("/") ? path.substring(1) : path;
        Path resolved = rootPath.resolve(sanitized).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new IOException("Path escapes root directory: " + path);
        }
        return resolved;
    }

    private String toRelativeOrAbsolutePath(Path resolved) {
        if (rootPath != null) {
            return rootPath.relativize(resolved).toString();
        }
        return resolved.toString();
    }
}
