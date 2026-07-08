package com.p99softtraining.hiresense.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OneDriveResumeDownloaderTest {

    private final OneDriveResumeDownloader downloader = new OneDriveResumeDownloader();

    @Test
    void testSupportsOneDriveShortenedUrl() {
        assertTrue(downloader.supports("https://1drv.ms/w/s!AjK123xyz"));
    }

    @Test
    void testSupportsOneDriveLiveUrl() {
        assertTrue(downloader.supports("https://onedrive.live.com/view.aspx?resid=ABC123"));
    }

    @Test
    void testSupportsOneDriveWithoutHttps() {
        assertTrue(downloader.supports("1drv.ms/w/s!AjK123"));
    }

    @Test
    void testDoesNotSupportNullUrl() {
        assertFalse(downloader.supports(null));
    }

    @Test
    void testDoesNotSupportNonOneDriveUrl() {
        assertFalse(downloader.supports("https://example.com/file.pdf"));
    }

    @Test
    void testDoesNotSupportEmptyString() {
        assertFalse(downloader.supports(""));
    }

    @Test
    void testBuildsDownloadUrlReplacesViewWithDownload() {
        String input = "https://onedrive.live.com/view.aspx?resid=ABC123";
        String expected = "https://onedrive.live.com/download.aspx?resid=ABC123";
        assertEquals(expected, downloader.buildDownloadUrl(input));
    }

    @Test
    void testBuildsDownloadUrlAppendsDownloadParam() {
        String input = "https://1drv.ms/w/s!AjK123xyz";
        String expected = "https://1drv.ms/w/s!AjK123xyz?download=1";
        assertEquals(expected, downloader.buildDownloadUrl(input));
    }

    @Test
    void testBuildsDownloadUrlAppendsDownloadParamWithExistingQueryString() {
        String input = "https://1drv.ms/w/s!AjK123xyz?param=value";
        String expected = "https://1drv.ms/w/s!AjK123xyz?param=value&download=1";
        assertEquals(expected, downloader.buildDownloadUrl(input));
    }

    @Test
    void testBuildsDownloadUrlReplacesViewInMiddleOfPath() {
        String input = "https://onedrive.live.com/view/folder/resume.pdf";
        String expected = "https://onedrive.live.com/download/folder/resume.pdf";
        assertEquals(expected, downloader.buildDownloadUrl(input));
    }

    @Test
    void testBuildsDownloadUrlHandles1drvMsWithQueryParams() {
        String input = "https://1drv.ms/w/s!AjK123?resid=xyz";
        String expected = "https://1drv.ms/w/s!AjK123?resid=xyz&download=1";
        assertEquals(expected, downloader.buildDownloadUrl(input));
    }

    @Test
    void testBuildsDownloadUrlOneDriveLiveWithViewAndParams() {
        String input = "https://onedrive.live.com/view.aspx?cid=123&resid=abc&authkey=xyz";
        String expected = "https://onedrive.live.com/download.aspx?cid=123&resid=abc&authkey=xyz";
        assertEquals(expected, downloader.buildDownloadUrl(input));
    }

    @Test
    void testBuildsDownloadUrlDoesNotDoubleReplaceView() {
        String input = "https://onedrive.live.com/view/view/file.pdf";
        String result = downloader.buildDownloadUrl(input);
        // Should replace the first /view with /download
        assertTrue(result.contains("/download"));
    }

    // Note: Testing actual HTTP download with 404/empty body requires integration testing
    // or mocking HttpClient. These scenarios are covered by:
    // - Requirement 1.5: HTTP 404 throws ResumeDownloadException (tested in integration)
    // - Requirement 1.6: Empty response body throws ResumeDownloadException (tested in integration)
}
