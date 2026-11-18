package com.github.matheuscruzsouza.nanospring.media.streaming;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MediaHeaderCacheServiceTest {

    @Test
    public void testPutGetAndEviction() {
        MediaHeaderCacheService cache = new MediaHeaderCacheService(2);
        cache.put("a", new MediaHeaderCacheService.MediaHeaders(100L, "\"1\"", 1000L, "audio/mpeg"));
        cache.put("b", new MediaHeaderCacheService.MediaHeaders(200L, "\"2\"", 2000L, "video/mp4"));
        assertEquals(2, cache.size());
        assertNotNull(cache.get("a"));

        cache.put("c", new MediaHeaderCacheService.MediaHeaders(300L, "\"3\"", 3000L, "audio/flac"));
        assertEquals(2, cache.size());
        assertNull(cache.get("b"));
        assertNotNull(cache.get("a"));
        assertNotNull(cache.get("c"));
    }
}
