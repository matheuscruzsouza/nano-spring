package com.github.matheuscruzsouza.nanospring.media.streaming;

/**
 * Parses and validates HTTP {@code Range} headers (RFC 7233 {@code bytes=...}).
 */
public class ByteRange {

    private final long start;
    private final long end;

    public ByteRange(long start, long end) {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid byte range: " + start + "-" + end);
        }
        this.start = start;
        this.end = end;
    }

    /**
     * Parses an HTTP {@code Range} header value against a known {@code totalLength}.
     * Supports {@code bytes=start-end}, {@code bytes=start-}, and {@code bytes=-suffix}.
     */
    public static ByteRange parse(String rangeHeader, long totalLength) {
        if (totalLength <= 0) {
            throw new IllegalArgumentException("Total length must be positive: " + totalLength);
        }
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            throw new IllegalArgumentException("Invalid Range header: " + rangeHeader);
        }
        String spec = rangeHeader.substring("bytes=".length()).trim();
        int commaIdx = spec.indexOf(',');
        if (commaIdx >= 0) {
            spec = spec.substring(0, commaIdx).trim();
        }
        int dashIdx = spec.indexOf('-');
        if (dashIdx < 0) {
            throw new IllegalArgumentException("Invalid Range spec: " + rangeHeader);
        }

        String startStr = spec.substring(0, dashIdx).trim();
        String endStr = spec.substring(dashIdx + 1).trim();

        long start;
        long end;

        if (startStr.isEmpty()) {
            if (endStr.isEmpty()) {
                throw new IllegalArgumentException("Empty Range spec: " + rangeHeader);
            }
            long suffixLength = Long.parseLong(endStr);
            if (suffixLength <= 0) {
                throw new IllegalArgumentException("Invalid suffix length: " + rangeHeader);
            }
            start = Math.max(0L, totalLength - suffixLength);
            end = totalLength - 1;
        } else {
            start = Long.parseLong(startStr);
            if (start < 0 || start >= totalLength) {
                throw new IllegalArgumentException("Range start out of bounds: " + start);
            }
            if (endStr.isEmpty()) {
                end = totalLength - 1;
            } else {
                end = Math.min(Long.parseLong(endStr), totalLength - 1);
            }
            if (end < start) {
                throw new IllegalArgumentException("Range end before start: " + start + "-" + end);
            }
        }
        return new ByteRange(start, end);
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long getLength() {
        return end - start + 1;
    }

    public String toContentRangeHeader(long totalLength) {
        return "bytes " + start + "-" + end + "/" + totalLength;
    }
}
