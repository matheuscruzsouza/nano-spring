package com.github.matheuscruzsouza.nanospring.media.streaming;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ByteRangeTest {

    @Test
    public void testParseExplicitRange() {
        ByteRange range = ByteRange.parse("bytes=10-49", 100L);
        assertEquals(10L, range.getStart());
        assertEquals(49L, range.getEnd());
        assertEquals(40L, range.getLength());
        assertEquals("bytes 10-49/100", range.toContentRangeHeader(100L));
    }

    @Test
    public void testParseOpenEndedRange() {
        ByteRange range = ByteRange.parse("bytes=50-", 100L);
        assertEquals(50L, range.getStart());
        assertEquals(99L, range.getEnd());
        assertEquals(50L, range.getLength());
    }

    @Test
    public void testParseSuffixRange() {
        ByteRange range = ByteRange.parse("bytes=-20", 100L);
        assertEquals(80L, range.getStart());
        assertEquals(99L, range.getEnd());
        assertEquals(20L, range.getLength());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseOutOfBoundsStartThrows() {
        ByteRange.parse("bytes=100-120", 100L);
    }
}
