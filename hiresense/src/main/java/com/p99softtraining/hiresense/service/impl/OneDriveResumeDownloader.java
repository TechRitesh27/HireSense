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

/**
 * Downloads resumes from OneDrive sharing links.
 *
 * Supports two patterns:
 *   1drv.ms/...            — shortened OneDrive share link
 *   onedrive.live.com/...  — full OneDrive sharing URL
 *
 * Transformation rules:
 *   - If the URL contains "/view", replace it with "/download"
 *   - Otherwise, append "&download=1" as a query parameter
 */
@Component
@Order(1)
public class OneDriveResumeDownloader implements ResumeDownloader {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Override
    public boolean supports(String url) {
        if (url == null) return false;
        return url.contains("1drv.ms") || url.contains("onedrive.live.com");
    }

    @Override
    public byte[] download(String url) throws ResumeDownloadException {
        String resolvedUrl = resolveRedirect(url);
        String downloadUrl = buildDownloadUrl(resolvedUrl);
        return fetchBytes(downloadUrl);
    }

    private String resolveRedirect(String url) {
        if (!url.contains("1drv.ms")) {
            return url;
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .connectTimeout(TIMEOUT)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                return response.headers().firstValue("Location").orElse(url);
            }
        } catch (Exception e) {
            // Fallback to original URL on lookup error
        }
        return url;
    }

    // Package-private for unit testing
    String buildDownloadUrl(String url) {
        if (url.contains("/view")) {
            return url.replace("/view", "/download");
        }
        // Append download=1 to force direct download
        if (url.contains("?")) {
            return url + "&download=1";
        } else {
            return url + "?download=1";
        }
    }

    private byte[] fetchBytes(String url) throws ResumeDownloadException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(TIMEOUT)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                throw new ResumeDownloadException(
                        "Failed to download resume from OneDrive. HTTP status: " + response.statusCode()
                );
            }

            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw new ResumeDownloadException("Downloaded resume from OneDrive was empty.");
            }

            return body;

        } catch (IOException e) {
            throw new ResumeDownloadException("Network error downloading from OneDrive: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResumeDownloadException("Download from OneDrive was interrupted.", e);
        }
    }
}
