package com.p99softtraining.hiresense.service.impl;

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NumericChars;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for GoogleDriveResumeDownloader URL transformation.
 *
 * Feature: resume-intelligence, Property 1: Google Drive URL transformation produces a valid export URL
 *
 * Validates: Requirements 1.3
 */
class GoogleDriveResumeDownloaderPropertyTest {

    private final GoogleDriveResumeDownloader downloader = new GoogleDriveResumeDownloader();

    /**
     * Property 1: For any alphanumeric Google Drive fileId, constructing a share URL and
     * calling supports() returns true, and buildDownloadUrl() produces the correct
     * export URL in the form https://drive.google.com/uc?export=download&id={fileId}.
     *
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    @Report(Reporting.GENERATED)
    void googleDriveUrlTransformationProducesValidExportUrl(
            @ForAll @AlphaChars @StringLength(min = 1, max = 40) String fileId) {

        // Construct a Google Drive share URL in the docs.google.com/file/d/{fileId}/view format
        String shareUrl = "https://docs.google.com/file/d/" + fileId + "/view";

        // Property: supports() must return true for any valid Google Drive share URL
        assertThat(downloader.supports(shareUrl))
                .as("supports() should return true for Google Drive share URL with fileId '%s'", fileId)
                .isTrue();

        // Property: buildDownloadUrl() must produce the correct usercontent download URL
        String exportUrl = downloader.buildDownloadUrl(shareUrl);
        String expectedExportUrl = "https://drive.usercontent.google.com/uc?id=" + fileId + "&export=download";

        assertThat(exportUrl)
                .as("buildDownloadUrl() should produce the correct usercontent URL for fileId '%s'", fileId)
                .isEqualTo(expectedExportUrl);
    }

    /**
     * Property 1 (variant): The same property holds for drive.google.com/file/d/{fileId}/ URLs.
     *
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    @Report(Reporting.GENERATED)
    void googleDriveUrlTransformationAlsoWorksForDriveSubdomain(
            @ForAll @AlphaChars @StringLength(min = 1, max = 40) String fileId) {

        // Construct a Google Drive share URL in the drive.google.com/file/d/{fileId}/view format
        String shareUrl = "https://drive.google.com/file/d/" + fileId + "/view?usp=sharing";

        // Property: supports() must return true
        assertThat(downloader.supports(shareUrl))
                .as("supports() should return true for drive.google.com share URL with fileId '%s'", fileId)
                .isTrue();

        // Property: export URL must use usercontent domain with correct fileId
        String exportUrl = downloader.buildDownloadUrl(shareUrl);
        String expectedExportUrl = "https://drive.usercontent.google.com/uc?id=" + fileId + "&export=download";

        assertThat(exportUrl)
                .as("buildDownloadUrl() should produce the correct usercontent URL for fileId '%s'", fileId)
                .isEqualTo(expectedExportUrl);
    }

    /**
     * Property 1 (fileId extraction): The fileId extracted from the URL is preserved exactly
     * in the export URL query parameter — no truncation, modification, or encoding.
     *
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    @Report(Reporting.GENERATED)
    void fileIdIsPreservedExactlyInExportUrl(
            @ForAll @AlphaChars @StringLength(min = 1, max = 33) String fileId) {

        String shareUrl = "https://docs.google.com/file/d/" + fileId + "/view";
        String exportUrl = downloader.buildDownloadUrl(shareUrl);

        // The export URL must use the usercontent domain
        assertThat(exportUrl)
                .as("Export URL should use drive.usercontent.google.com domain")
                .startsWith("https://drive.usercontent.google.com/uc?id=");

        // The export URL must contain the fileId exactly as the id= parameter value
        assertThat(exportUrl)
                .as("Export URL should contain the exact fileId as the 'id' parameter")
                .contains("id=" + fileId);
    }

    /**
     * Property 1 (open?id= variant): drive.google.com/open?id={fileId} URLs are supported
     * and transform to the correct usercontent download URL.
     *
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    @Report(Reporting.GENERATED)
    void googleDriveOpenIdUrlTransformationProducesValidExportUrl(
            @ForAll @AlphaChars @StringLength(min = 1, max = 40) String fileId) {

        String shareUrl = "https://drive.google.com/open?id=" + fileId;

        // Property: supports() must return true for open?id= URLs
        assertThat(downloader.supports(shareUrl))
                .as("supports() should return true for drive.google.com/open?id= URL with fileId '%s'", fileId)
                .isTrue();

        // Property: buildDownloadUrl() must produce the correct usercontent download URL
        String exportUrl = downloader.buildDownloadUrl(shareUrl);
        String expectedExportUrl = "https://drive.usercontent.google.com/uc?id=" + fileId + "&export=download";

        assertThat(exportUrl)
                .as("buildDownloadUrl() should produce the correct usercontent URL for open?id= format with fileId '%s'", fileId)
                .isEqualTo(expectedExportUrl);
    }
}
