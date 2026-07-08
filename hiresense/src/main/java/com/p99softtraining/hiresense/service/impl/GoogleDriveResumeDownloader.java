package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.exception.ResumeDownloadException;
import com.p99softtraining.hiresense.service.ResumeDownloader;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads resumes from Google Drive sharing links.
 *
 * Supports three patterns:
 *   docs.google.com/file/d/{fileId}/   — native file share
 *   drive.google.com/file/d/{fileId}/  — Drive file share
 *   drive.google.com/open?id={fileId}  — older "open" share link
 *
 * All are transformed to the direct download URL on drive.usercontent.google.com:
 *   https://drive.usercontent.google.com/uc?id={fileId}&export=download
 *
 * Note: drive.google.com/uc?export=download was deprecated and now returns
 * a viewer HTML page. The usercontent subdomain is the current working endpoint.
 */
@Component
@Order(1)
public class GoogleDriveResumeDownloader implements ResumeDownloader {

    /** Matches /file/d/{fileId} style URLs from docs.google.com or drive.google.com */
    private static final Pattern DRIVE_FILE_PATTERN =
            Pattern.compile("(?:docs|drive)\\.google\\.com/file/d/([a-zA-Z0-9_-]+)");

    /** Matches drive.google.com/open?id={fileId} style URLs */
    private static final Pattern DRIVE_OPEN_PATTERN =
            Pattern.compile("drive\\.google\\.com/open\\?.*\\bid=([a-zA-Z0-9_-]+)");

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Override
    public boolean supports(String url) {
        if (url == null) return false;
        return DRIVE_FILE_PATTERN.matcher(url).find()
                || DRIVE_OPEN_PATTERN.matcher(url).find();
    }

    @Override
    public byte[] download(String url) throws ResumeDownloadException {
        String downloadUrl = buildDownloadUrl(url);
        return fetchBytes(downloadUrl);
    }

    // Package-private for unit testing
    String buildDownloadUrl(String url) {
        Matcher fileMatcher = DRIVE_FILE_PATTERN.matcher(url);
        if (fileMatcher.find()) {
            return "https://drive.usercontent.google.com/uc?id=" + fileMatcher.group(1) + "&export=download";
        }
        Matcher openMatcher = DRIVE_OPEN_PATTERN.matcher(url);
        if (openMatcher.find()) {
            return "https://drive.usercontent.google.com/uc?id=" + openMatcher.group(1) + "&export=download";
        }
        throw new ResumeDownloadException("Cannot extract file ID from Google Drive URL: " + url);
    }

    private byte[] fetchBytes(String url) throws ResumeDownloadException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(TIMEOUT)
                .build();

        try {
            // First attempt — append confirm=t to bypass the virus-scan warning page
            // that Google Drive shows for files larger than ~25 MB.
            String confirmedUrl = url.contains("?")
                    ? url + "&confirm=t"
                    : url + "?confirm=t";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(confirmedUrl))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Cookie", "download_warning=t")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                throw new ResumeDownloadException(
                        "Failed to download resume from Google Drive. HTTP status: " + response.statusCode()
                );
            }

            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw new ResumeDownloadException("Downloaded resume from Google Drive was empty.");
            }

            // Detect if Google returned an HTML confirmation/warning page instead of the file.
            // HTML pages start with '<' (0x3C) or whitespace; real PDF/DOCX start with %PDF or PK.
            if (isHtmlContent(body)) {
                throw new ResumeDownloadException(
                        "Google Drive returned an HTML page instead of the file. " +
                        "Ensure the file is shared with 'Anyone with the link' and is accessible."
                );
            }

            return body;

        } catch (IOException e) {
            throw new ResumeDownloadException("Network error downloading from Google Drive: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResumeDownloadException("Download from Google Drive was interrupted.", e);
        }
    }

    /**
     * Returns true if the byte array looks like an HTML page rather than a binary file.
     * Checks for leading whitespace followed by '<', or a direct '<' at the start.
     */
    private boolean isHtmlContent(byte[] bytes) {
        if (bytes == null || bytes.length < 5) return false;
        // Skip BOM or whitespace
        int i = 0;
        while (i < bytes.length && (bytes[i] == ' ' || bytes[i] == '\n' || bytes[i] == '\r'
                || bytes[i] == '\t' || bytes[i] == (byte) 0xEF)) {
            i++;
        }
        // Check for '<' which starts HTML, or 'G' which often starts Google's error JSON
        return i < bytes.length && (bytes[i] == '<' || (bytes[i] == '{'));
    }
}
