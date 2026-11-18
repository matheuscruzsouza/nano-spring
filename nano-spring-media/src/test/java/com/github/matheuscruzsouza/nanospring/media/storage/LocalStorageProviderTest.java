package com.github.matheuscruzsouza.nanospring.media.storage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalStorageProviderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testExistsStatAndList() throws IOException {
        File root = tempFolder.getRoot();
        File subDir = tempFolder.newFolder("media");
        File file = new File(subDir, "song.mp3");
        byte[] content = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
        Files.write(file.toPath(), content);

        LocalStorageProvider provider = new LocalStorageProvider(root);
        assertTrue(provider.exists("media/song.mp3"));
        assertFalse(provider.exists("media/missing.mp3"));

        FileEntry entry = provider.stat("media/song.mp3");
        assertEquals("song.mp3", entry.getName());
        assertEquals(16L, entry.getSize());
        assertFalse(entry.isDirectory());

        List<FileEntry> entries = provider.list("media");
        assertEquals(1, entries.size());
        assertEquals("song.mp3", entries.get(0).getName());
    }

    @Test
    public void testOpenStreamWithOffset() throws IOException {
        File root = tempFolder.getRoot();
        File file = tempFolder.newFile("video.mp4");
        Files.write(file.toPath(), "HelloWorld!".getBytes(StandardCharsets.UTF_8));

        LocalStorageProvider provider = new LocalStorageProvider(root);
        try (InputStream is = provider.openStream("video.mp4", 5L)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            assertEquals("World!", baos.toString(StandardCharsets.UTF_8.name()));
        }
    }

    @Test(expected = IOException.class)
    public void testPathTraversalRejected() throws IOException {
        LocalStorageProvider provider = new LocalStorageProvider(tempFolder.getRoot());
        provider.exists("../etc/passwd");
    }
}
