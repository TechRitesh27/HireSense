package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.exception.ResumeDownloadException;
import com.p99softtraining.hiresense.service.ResumeDownloader;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fallback downloader for any standard http:// or https:// URL.
 *
 * No URL transformation is applied — the URL is fetched directly.
 * Registered at {@link Ordered#LOWEST_PRECEDENCE} so that more specific
 * implementations (Google Drive, OneDrive) are evaluated first.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GeneralHttpResumeDownloader implements ResumeDownloader {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Override
    public boolean supports(String url) {
        if (url == null) return false;
        boolean isHttp = url.startsWith("http://") || url.startsWith("https://");
        boolean isGoogleDrive = url.contains("drive.google.com") || url.contains("docs.google.com");
        boolean isOneDrive = url.contains("1drv.ms") || url.contains("onedrive.live.com");
        return isHttp && !isGoogleDrive && !isOneDrive;
    }

    @Override
    public byte[] download(String url) throws ResumeDownloadException {
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
                        "Failed to download resume. HTTP status: " + response.statusCode()
                );
            }

            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw new ResumeDownloadException("Downloaded resume was empty.");
            }

            return body;

        } catch (IOException e) {
            throw new ResumeDownloadException("Network error downloading resume: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResumeDownloadException("Resume download was interrupted.", e);
        }
    }
}
