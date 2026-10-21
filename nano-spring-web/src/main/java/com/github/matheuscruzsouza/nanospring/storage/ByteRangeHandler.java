package com.github.matheuscruzsouza.nanospring.storage;

import com.github.matheuscruzsouza.nanospring.http.HttpStatus;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

import java.io.File;
import java.io.InputStream;

public final class ByteRangeHandler {

    private ByteRangeHandler() {
    }

    public static Response serveFile(IHTTPSession session, File file, String mimeType) {
        if (file == null || !file.exists()) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found");
        }
        return serveResource(session, new FileResourceRegion(file, mimeType));
    }

    public static Response serveResource(IHTTPSession session, ResourceRegion resource) {
        long totalLength = resource.contentLength();
        String rangeHeader = (session != null && session.getHeaders() != null)
                ? session.getHeaders().get("range")
                : null;
        String mimeType = resource.contentType();
        if (mimeType == null || mimeType.trim().isEmpty()) {
            mimeType = "application/octet-stream";
        }

        // 1. Caso sem header Range: retorna o arquivo integralmente (200 OK)
        if (rangeHeader == null || !rangeHeader.trim().toLowerCase().startsWith("bytes=")) {
            try {
                InputStream in = resource.openStream();
                Response response = NanoHTTPD.newFixedLengthResponse(
                        Response.Status.OK,
                        mimeType,
                        in,
                        totalLength
                );
                response.addHeader("Accept-Ranges", "bytes");
                response.addHeader("Content-Length", String.valueOf(totalLength));
                return response;
            } catch (Exception e) {
                closeQuietly(resource);
                return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Erro lendo recurso: " + e.getMessage());
            }
        }

        // 2. Processa os limites de bytes (ex: "bytes=100-200" ou "bytes=500-" ou "bytes=-500")
        String rangeSpec = rangeHeader.trim().substring("bytes=".length()).trim();

        // Se contiver múltiplas faixas (ex: bytes=0-50, 100-150), pega a primeira faixa por simplificação
        if (rangeSpec.contains(",")) {
            rangeSpec = rangeSpec.split(",")[0].trim();
        }

        int dashIndex = rangeSpec.indexOf('-');
        if (dashIndex == -1) {
            return createRangeNotSatisfiable(resource, totalLength);
        }

        long start = 0;
        long end = totalLength - 1;

        try {
            String startPart = rangeSpec.substring(0, dashIndex).trim();
            String endPart = rangeSpec.substring(dashIndex + 1).trim();

            if (!startPart.isEmpty() && !endPart.isEmpty()) {
                start = Long.parseLong(startPart);
                end = Long.parseLong(endPart);
            } else if (!startPart.isEmpty()) {
                start = Long.parseLong(startPart);
            } else if (!endPart.isEmpty()) {
                // Suffix range: bytes=-500 (últimos 500 bytes)
                long suffix = Long.parseLong(endPart);
                if (suffix > totalLength) {
                    suffix = totalLength;
                }
                start = Math.max(0, totalLength - suffix);
                end = totalLength - 1;
            } else {
                return createRangeNotSatisfiable(resource, totalLength);
            }
        } catch (NumberFormatException e) {
            return createRangeNotSatisfiable(resource, totalLength);
        }

        // 3. Validação dos limites
        if (totalLength == 0 || start > end || start >= totalLength || start < 0) {
            return createRangeNotSatisfiable(resource, totalLength);
        }

        if (end >= totalLength) {
            end = totalLength - 1;
        }

        long chunkLength = end - start + 1;

        // 4. Efetua o Seek sem carregar o conteúdo na memória heap
        try {
            resource.seek(start);
            InputStream bounded = new BoundedInputStream(resource.openStream(), chunkLength, resource);

            Response response = NanoHTTPD.newFixedLengthResponse(
                    HttpStatus.PARTIAL_CONTENT,
                    mimeType,
                    bounded,
                    chunkLength
            );

            response.addHeader("Accept-Ranges", "bytes");
            response.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + totalLength);
            response.addHeader("Content-Length", String.valueOf(chunkLength));

            return response;
        } catch (Exception e) {
            closeQuietly(resource);
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Erro ao processar stream parcial: " + e.getMessage());
        }
    }

    private static Response createRangeNotSatisfiable(AutoCloseable resource, long totalLength) {
        closeQuietly(resource);
        Response res = NanoHTTPD.newFixedLengthResponse(HttpStatus.RANGE_NOT_SATISFIABLE, "text/plain", "Range Not Satisfiable");
        res.addHeader("Content-Range", "bytes */" + totalLength);
        return res;
    }

    private static void closeQuietly(AutoCloseable res) {
        if (res != null) {
            try {
                res.close();
            } catch (Exception ignored) {
            }
        }
    }
}
