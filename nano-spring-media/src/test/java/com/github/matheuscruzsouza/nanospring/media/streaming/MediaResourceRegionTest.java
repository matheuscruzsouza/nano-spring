package com.github.matheuscruzsouza.nanospring.media.streaming;

import com.github.matheuscruzsouza.nanospring.media.storage.LocalStorageProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class MediaResourceRegionTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testSeekAndOpenStream() throws IOException {
        File file = tempFolder.newFile("track.flac");
        byte[] data = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(StandardCharsets.UTF_8);
        Files.write(file.toPath(), data);

        LocalStorageProvider provider = new LocalStorageProvider(tempFolder.getRoot());
        try (MediaResourceRegion region = new MediaResourceRegion(provider, "track.flac", data.length, "audio/flac")) {
            assertEquals(26L, region.contentLength());
            assertEquals("audio/flac", region.contentType());

            region.seek(20L);
            try (InputStream is = region.openStream()) {
                byte[] buf = new byte[6];
                int read = is.read(buf);
                assertEquals(6, read);
                assertEquals("UVWXYZ", new String(buf, StandardCharsets.UTF_8));
            }
        }
    }
}
