package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.service.SpreadsheetDownloader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GoogleSheetsDownloader implements SpreadsheetDownloader {

    private static final Pattern GOOGLE_SHEETS_PATTERN = Pattern.compile("docs\\.google\\.com/spreadsheets/d/([a-zA-Z0-9-_]+)");
    private static final Pattern GID_PATTERN = Pattern.compile("(?:[?&]gid=|#gid=)([0-9]+)");

    @Override
    public boolean supports(String url) {
        if (url == null) {
            return false;
        }
        return GOOGLE_SHEETS_PATTERN.matcher(url).find();
    }

    @Override
    public InputStream download(String url) throws IOException {
        String exportUrl = buildExportUrl(url);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(exportUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/octet-stream, */*;q=0.8")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body();

            if (response.statusCode() != 200) {
                throw new IOException("Failed to download Google Sheet. HTTP status: " + response.statusCode());
            }
            if (body == null || body.length == 0) {
                throw new IOException("Downloaded Google Sheet was empty.");
            }

            // Always check for HTML BEFORE returning the stream — POI gives a confusing
            // "unsupported file type: HTML" error if we don't catch it here first.
            if (looksLikeHtml(body)) {
                throw new IOException(
                    "Google Sheets returned a login page instead of the spreadsheet. " +
                    "Open the sheet → Share → 'Anyone with the link' → Viewer, then try again."
                );
            }

            return new java.io.ByteArrayInputStream(body);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
    }

    String buildExportUrl(String url) {
        Matcher matcher = GOOGLE_SHEETS_PATTERN.matcher(url);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid Google Sheets URL format");
        }

        String spreadsheetId = matcher.group(1);
        String gid = extractGid(url);
        // Correct export URL — no redundant 'id=' param which can trigger a redirect
        StringBuilder exportUrl = new StringBuilder("https://docs.google.com/spreadsheets/d/")
                .append(spreadsheetId)
                .append("/export?format=xlsx");

        if (gid != null && !gid.isBlank()) {
            exportUrl.append("&gid=").append(gid);
        }

        return exportUrl.toString();
    }

    private String extractGid(String url) {
        Matcher matcher = GID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private boolean looksLikeHtml(byte[] body) {
        // Decode first 512 bytes, strip BOM and whitespace, then check for HTML markers
        int checkLen = Math.min(body.length, 512);
        String content = new String(body, 0, checkLen, java.nio.charset.StandardCharsets.UTF_8)
                .replace("\uFEFF", "")  // strip UTF-8 BOM
                .stripLeading()
                .toLowerCase();
        return content.startsWith("<!doctype")
                || content.startsWith("<html")
                || content.startsWith("<body")
                || content.contains("<title>");
    }
}
