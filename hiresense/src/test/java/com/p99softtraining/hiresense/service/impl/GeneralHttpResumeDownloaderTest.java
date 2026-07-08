package com.p99softtraining.hiresense.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneralHttpResumeDownloaderTest {

    private final GeneralHttpResumeDownloader downloader = new GeneralHttpResumeDownloader();

    @Test
    void testSupportsHttpsUrl() {
        assertTrue(downloader.supports("https://example.com/resume.pdf"));
    }

    @Test
    void testSupportsHttpUrl() {
        assertTrue(downloader.supports("http://example.com/file.docx"));
    }

    @Test
    void testDoesNotSupportNullUrl() {
        assertFalse(downloader.supports(null));
    }

    @Test
    void testDoesNotSupportEmptyString() {
        assertFalse(downloader.supports(""));
    }

    @Test
    void testDoesNotSupportFtpUrl() {
        assertFalse(downloader.supports("ftp://example.com/file.pdf"));
    }

    @Test
    void testDoesNotSupportFileUrl() {
        assertFalse(downloader.supports("file:///path/to/file.pdf"));
    }

    @Test
    void testDoesNotSupportRelativeUrl() {
        assertFalse(downloader.supports("/relative/path/file.pdf"));
    }

    @Test
    void testSupportsHttpsWithPort() {
        assertTrue(downloader.supports("https://example.com:8443/resume.pdf"));
    }

    @Test
    void testSupportsHttpWithQueryParams() {
        assertTrue(downloader.supports("http://example.com/file.pdf?download=true&token=abc123"));
    }

    @Test
    void testSupportsHttpsWithFragment() {
        assertTrue(downloader.supports("https://example.com/docs/resume.pdf#section1"));
    }

    @Test
    void testSupportsComplexHttpsUrl() {
        assertTrue(downloader.supports("https://cdn.example.com:8080/files/resumes/john-doe.pdf?v=2&auth=token#page1"));
    }

    // Note: Testing actual HTTP download with 404/empty body requires integration testing
    // or mocking HttpClient. These scenarios are covered by:
    // - Requirement 1.5: HTTP 404 throws ResumeDownloadException (tested in integration)
    // - Requirement 1.6: Empty response body throws ResumeDownloadException (tested in integration)
    // 
    // Integration test scenarios to be covered:
    // - HTTP 404 status code throws ResumeDownloadException
    // - HTTP 500 status code throws ResumeDownloadException
    // - Empty response body (0 bytes) throws ResumeDownloadException
    // - Null response body throws ResumeDownloadException
    // - Timeout throws ResumeDownloadException
    // - Network IOException throws ResumeDownloadException
}
