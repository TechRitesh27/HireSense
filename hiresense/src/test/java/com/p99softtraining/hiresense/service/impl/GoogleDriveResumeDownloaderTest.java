package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.exception.ResumeDownloadException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoogleDriveResumeDownloaderTest {

    private final GoogleDriveResumeDownloader downloader = new GoogleDriveResumeDownloader();

    @Test
    void testSupportsGoogleDocsShareUrl() {
        assertTrue(downloader.supports("https://docs.google.com/file/d/1BxiMVs0XRA5nFMdKvBdBZpA/view"));
    }

    @Test
    void testSupportsGoogleDriveShareUrl() {
        assertTrue(downloader.supports("https://drive.google.com/file/d/1BxiMVs0XRA5nFMdKvBdBZpA/view?usp=sharing"));
    }

    @Test
    void testSupportsGoogleDocsWithoutHttps() {
        assertTrue(downloader.supports("docs.google.com/file/d/abc123-xyz/edit"));
    }

    @Test
    void testSupportsFileIdWithHyphensAndUnderscores() {
        assertTrue(downloader.supports("https://docs.google.com/file/d/abc_123-XYZ_789/view"));
    }

    @Test
    void testDoesNotSupportNullUrl() {
        assertFalse(downloader.supports(null));
    }

    @Test
    void testDoesNotSupportNonGoogleDriveUrl() {
        assertFalse(downloader.supports("https://example.com/resume.pdf"));
    }

    @Test
    void testDoesNotSupportEmptyString() {
        assertFalse(downloader.supports(""));
    }

    @Test
    void testBuildsDownloadUrlFromDocsGoogleComUrl() {
        String input = "https://docs.google.com/file/d/1BxiMVs0XRA5nFMdKvBdBZpA/view";
        String expected = "https://drive.usercontent.google.com/uc?id=1BxiMVs0XRA5nFMdKvBdBZpA&export=download";
        assertEquals(expected, downloader.buildDownloadUrl(input));
    }

    @Test
    void testBuildsDownloadUrlFromDriveGoogleComUrl() {
        String input = "https://drive.google.com/file/d/abc123XYZ-_789/view?usp=sharing";
        String expected = "https://drive.usercontent.google.com/uc?id=abc123XYZ-_789&export=download";
        assertEquals(expected, downloader.buildDownloadUrl(input));
    }

    @Test
    void testBuildsDownloadUrlWithoutHttpsPrefix() {
        String input = "docs.google.com/file/d/fileId123/edit";
        String expected = "https://drive.usercontent.google.com/uc?id=fileId123&export=download";
        assertEquals(expected, downloader.buildDownloadUrl(input));
    }

    @Test
    void testBuildsDownloadUrlExtractsCorrectFileId() {
        String input = "https://docs.google.com/file/d/1a2b3c4d5e6f7g8h9i0j/view";
        String result = downloader.buildDownloadUrl(input);
        assertTrue(result.contains("id=1a2b3c4d5e6f7g8h9i0j"));
    }

    @Test
    void testBuildDownloadUrlThrowsExceptionForInvalidUrl() {
        String invalidUrl = "https://example.com/notgoogledrive";
        ResumeDownloadException exception = assertThrows(
                ResumeDownloadException.class,
                () -> downloader.buildDownloadUrl(invalidUrl)
        );
        assertTrue(exception.getMessage().contains("Cannot extract file ID"));
    }

    @Test
    void testBuildsDownloadUrlHandlesComplexShareUrl() {
        String input = "https://drive.google.com/file/d/1BxiMVs0XRA5nFMdKvBdBZpA/view?usp=sharing&resourcekey=0-xyz";
        String expected = "https://drive.usercontent.google.com/uc?id=1BxiMVs0XRA5nFMdKvBdBZpA&export=download";
        assertEquals(expected, downloader.buildDownloadUrl(input));
    }

    // Note: Testing actual HTTP download with 404/empty body requires integration testing
    // or mocking HttpClient. These scenarios are covered by:
    // - Requirement 1.5: HTTP 404 throws ResumeDownloadException (tested in integration)
    // - Requirement 1.6: Empty response body throws ResumeDownloadException (tested in integration)
}
