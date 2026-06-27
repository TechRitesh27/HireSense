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
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/octet-stream, */*;q=0.8")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body();
            if (response.statusCode() != 200) {
                throw new IOException("Failed to download Google Sheet. HTTP status code: " + response.statusCode());
            }
            if (body == null || body.length == 0) {
                throw new IOException("Downloaded Google Sheet was empty");
            }
            if (looksLikeHtml(body)) {
                throw new IOException("Google Sheets export returned HTML instead of spreadsheet data");
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
        StringBuilder exportUrl = new StringBuilder("https://docs.google.com/spreadsheets/d/")
                .append(spreadsheetId)
                .append("/export?format=xlsx&id=")
                .append(spreadsheetId);

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
        String content = new String(body, java.nio.charset.StandardCharsets.UTF_8).trim();
        return content.startsWith("<!DOCTYPE") || content.startsWith("<html") || content.startsWith("<body") || content.contains("<title>");
    }
}
