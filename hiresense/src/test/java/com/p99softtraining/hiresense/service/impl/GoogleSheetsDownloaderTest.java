package com.p99softtraining.hiresense.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoogleSheetsDownloaderTest {

    private final GoogleSheetsDownloader downloader = new GoogleSheetsDownloader();

    @Test
    void testSupportsValidGoogleSheetsUrls() {
        assertTrue(downloader.supports("https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBipkA4d1ACTpD5XNjhfA/edit"));
        assertTrue(downloader.supports("https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBipkA4d1ACTpD5XNjhfA/edit?usp=sharing"));
        assertTrue(downloader.supports("docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBipkA4d1ACTpD5XNjhfA"));
    }

    @Test
    void testBuildsExportUrlForSharedGoogleSheetsLinks() {
        // The export URL intentionally omits the redundant 'id=' param — see GoogleSheetsDownloader comment
        assertEquals(
                "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBipkA4d1ACTpD5XNjhfA/export?format=xlsx&gid=0",
                downloader.buildExportUrl("https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBipkA4d1ACTpD5XNjhfA/edit?usp=sharing&gid=0")
        );
    }

    @Test
    void testDoesNotSupportInvalidUrls() {
        assertFalse(downloader.supports("https://example.com/sheet.xlsx"));
        assertFalse(downloader.supports(null));
        assertFalse(downloader.supports(""));
    }
}
