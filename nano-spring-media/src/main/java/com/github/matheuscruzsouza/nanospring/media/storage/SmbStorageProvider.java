package com.github.matheuscruzsouza.nanospring.media.storage;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * {@link StorageProvider} implementation for SMB/CIFS shares using {@code smbj}.
 */
public class SmbStorageProvider implements StorageProvider, Closeable {

    private final String host;
    private final String shareName;
    private final AuthenticationContext authContext;

    private SMBClient client;
    private Connection connection;
    private Session session;
    private DiskShare share;
    private final boolean ownsShare;

    public SmbStorageProvider(String host, String shareName, String username, String password) {
        this(host, shareName, username, password, "");
    }

    public SmbStorageProvider(String host, String shareName, String username, String password, String domain) {
        this.host = host;
        this.shareName = shareName;
        char[] passChars = password != null ? password.toCharArray() : new char[0];
        this.authContext = new AuthenticationContext(
                username != null ? username : "",
                passChars,
                domain != null ? domain : ""
        );
        this.ownsShare = true;
    }

    public SmbStorageProvider(DiskShare share) {
        this.host = null;
        this.shareName = null;
        this.authContext = null;
        this.share = share;
        this.ownsShare = false;
    }

    private synchronized DiskShare getShare() throws IOException {
        if (share != null && share.isConnected()) {
            return share;
        }
        if (!ownsShare) {
            throw new IOException("Provided DiskShare is not connected");
        }
        close();
        client = new SMBClient();
        connection = client.connect(host);
        session = connection.authenticate(authContext);
        share = (DiskShare) session.connectShare(shareName);
        return share;
    }

    @Override
    public boolean exists(String path) throws IOException {
        String smbPath = normalizeSmbPath(path);
        DiskShare diskShare = getShare();
        return diskShare.fileExists(smbPath) || diskShare.folderExists(smbPath);
    }

    @Override
    public FileEntry stat(String path) throws IOException {
        String smbPath = normalizeSmbPath(path);
        DiskShare diskShare = getShare();
        FileAllInformation info = diskShare.getFileInformation(smbPath);
        boolean isDir = (info.getBasicInformation().getFileAttributes()
                & FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue()) != 0;
        long size = isDir ? 0L : info.getStandardInformation().getEndOfFile();
        long lastModified = info.getBasicInformation().getLastWriteTime().toEpochMillis();
        String name = extractName(smbPath);
        return new FileEntry(name, smbPath, size, isDir, lastModified);
    }

    @Override
    public List<FileEntry> list(String path) throws IOException {
        String smbPath = normalizeSmbPath(path);
        DiskShare diskShare = getShare();
        List<FileIdBothDirectoryInformation> items = diskShare.list(smbPath);
        List<FileEntry> entries = new ArrayList<>();
        for (FileIdBothDirectoryInformation item : items) {
            String name = item.getFileName();
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }
            boolean isDir = (item.getFileAttributes()
                    & FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue()) != 0;
            long size = isDir ? 0L : item.getEndOfFile();
            long lastModified = item.getLastWriteTime().toEpochMillis();
            String childPath = smbPath.isEmpty() ? name : smbPath + "\\" + name;
            entries.add(new FileEntry(name, childPath, size, isDir, lastModified));
        }
        return entries;
    }

    @Override
    public InputStream openStream(String path, long offset) throws IOException {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative: " + offset);
        }
        String smbPath = normalizeSmbPath(path);
        DiskShare diskShare = getShare();
        File smbFile = diskShare.openFile(
                smbPath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
        );
        return new SmbOffsetInputStream(smbFile, offset);
    }

    @Override
    public synchronized void close() throws IOException {
        if (!ownsShare) {
            return;
        }
        IOException firstError = null;
        if (share != null) {
            try {
                share.close();
            } catch (IOException e) {
                firstError = e;
            }
            share = null;
        }
        if (session != null) {
            try {
                session.close();
            } catch (IOException e) {
                if (firstError == null) {
                    firstError = e;
                }
            }
            session = null;
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                if (firstError == null) {
                    firstError = e;
                }
            }
            connection = null;
        }
        if (client != null) {
            client.close();
            client = null;
        }
        if (firstError != null) {
            throw firstError;
        }
    }

    private static String normalizeSmbPath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.replace('/', '\\');
        while (normalized.startsWith("\\")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String extractName(String smbPath) {
        if (smbPath.isEmpty()) {
            return "";
        }
        int idx = smbPath.lastIndexOf('\\');
        return idx >= 0 ? smbPath.substring(idx + 1) : smbPath;
    }

    private static final class SmbOffsetInputStream extends InputStream {

        private final File smbFile;
        private long position;
        private boolean closed = false;

        SmbOffsetInputStream(File smbFile, long initialOffset) {
            this.smbFile = smbFile;
            this.position = initialOffset;
        }

        @Override
        public int read() throws IOException {
            byte[] single = new byte[1];
            int n = read(single, 0, 1);
            return n == -1 ? -1 : (single[0] & 0xFF);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (closed) {
                throw new IOException("Stream is closed");
            }
            if (len == 0) {
                return 0;
            }
            int bytesRead = smbFile.read(b, position, off, len);
            if (bytesRead <= 0) {
                return -1;
            }
            position += bytesRead;
            return bytesRead;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                smbFile.close();
            }
        }
    }
}
