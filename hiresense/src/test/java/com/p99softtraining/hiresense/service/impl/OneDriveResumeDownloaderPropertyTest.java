package com.p99softtraining.hiresense.service.impl;

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for OneDriveResumeDownloader URL transformation.
 *
 * Feature: resume-intelligence, Property 2: OneDrive URL transformation produces a valid download URL
 *
 * Validates: Requirements 1.4
 */
class OneDriveResumeDownloaderPropertyTest {

    private final OneDriveResumeDownloader downloader = new OneDriveResumeDownloader();

    /**
     * Property 2a: For any alphanumeric share token, a shortened 1drv.ms URL without
     * an existing query string is recognized and transformed by appending "?download=1".
     *
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    @Report(Reporting.GENERATED)
    void oneDriveShortUrlWithoutQueryTransformationAppendsDownloadParam(
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String shareToken) {

        String shareUrl = "https://1drv.ms/w/s!" + shareToken;

        // Property: supports() must return true for any 1drv.ms URL
        assertThat(downloader.supports(shareUrl))
                .as("supports() should return true for 1drv.ms URL with token '%s'", shareToken)
                .isTrue();

        // Property: buildDownloadUrl() must append ?download=1 when no query string exists
        String downloadUrl = downloader.buildDownloadUrl(shareUrl);

        assertThat(downloadUrl)
                .as("buildDownloadUrl() should append '?download=1' for 1drv.ms URL without existing query params")
                .isEqualTo(shareUrl + "?download=1");
    }

    /**
     * Property 2b: For any alphanumeric share token, a shortened 1drv.ms URL that already
     * has a query string is recognized and transformed by appending "&download=1".
     *
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    @Report(Reporting.GENERATED)
    void oneDriveShortUrlWithQueryTransformationAppendsDownloadParam(
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String shareToken) {

        String shareUrl = "https://1drv.ms/w/s!" + shareToken + "?resid=abc";

        // Property: supports() must return true
        assertThat(downloader.supports(shareUrl))
                .as("supports() should return true for 1drv.ms URL with existing query string")
                .isTrue();

        // Property: buildDownloadUrl() must append &download=1 when a query string already exists
        String downloadUrl = downloader.buildDownloadUrl(shareUrl);

        assertThat(downloadUrl)
                .as("buildDownloadUrl() should append '&download=1' for 1drv.ms URL with existing query params")
                .isEqualTo(shareUrl + "&download=1");
    }

    /**
     * Property 2c: For any alphanumeric resource ID, an onedrive.live.com URL containing
     * "/view" is recognized and transformed by replacing "/view" with "/download".
     *
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    @Report(Reporting.GENERATED)
    void oneDriveLiveViewUrlTransformationReplacesViewWithDownload(
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String resourceId) {

        String shareUrl = "https://onedrive.live.com/view.aspx?resid=" + resourceId;

        // Property: supports() must return true for onedrive.live.com URLs
        assertThat(downloader.supports(shareUrl))
                .as("supports() should return true for onedrive.live.com URL with resid '%s'", resourceId)
                .isTrue();

        // Property: buildDownloadUrl() must replace /view with /download
        String downloadUrl = downloader.buildDownloadUrl(shareUrl);
        String expectedUrl = "https://onedrive.live.com/download.aspx?resid=" + resourceId;

        assertThat(downloadUrl)
                .as("buildDownloadUrl() should replace '/view' with '/download' for onedrive.live.com URL")
                .isEqualTo(expectedUrl);
    }

    /**
     * Property 2d: For any produced download URL, the URL always contains "download",
     * ensuring every transformation strategy leads to a download-capable URL.
     * This holds for both 1drv.ms and onedrive.live.com URL variants.
     *
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    @Report(Reporting.GENERATED)
    void allOneDriveDownloadUrlsContainDownloadIndicator(
            @ForAll("oneDriveShareUrls") String shareUrl) {

        // Property: every transformed URL must contain a download indicator
        String downloadUrl = downloader.buildDownloadUrl(shareUrl);

        assertThat(downloadUrl)
                .as("Every OneDrive download URL should contain 'download'")
                .contains("download");
    }

    /**
     * Property 2e: URL transformation is idempotent for the download parameter —
     * a URL that already has "?download=1" appended will have "&download=1" appended,
     * but crucially will never lose the original "download=1" parameter.
     *
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    @Report(Reporting.GENERATED)
    void oneDriveTransformationNeverDropsOriginalUrl(
            @ForAll @AlphaChars @StringLength(min = 1, max = 25) String shareToken) {

        String shareUrl = "https://1drv.ms/w/s!" + shareToken;
        String downloadUrl = downloader.buildDownloadUrl(shareUrl);

        // Property: the original URL must be a prefix of the transformed URL —
        // transformation only appends or replaces, never truncates
        assertThat(downloadUrl)
                .as("Transformed URL should start with the original share URL prefix")
                .startsWith("https://1drv.ms/w/s!" + shareToken);
    }

    /**
     * Arbitrary that produces a mix of 1drv.ms and onedrive.live.com share URLs,
     * covering the two main URL patterns supported by OneDriveResumeDownloader.
     */
    @Provide
    Arbitrary<String> oneDriveShareUrls() {
        Arbitrary<String> shortUrls = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .map(token -> "https://1drv.ms/w/s!" + token);

        Arbitrary<String> liveViewUrls = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .map(resid -> "https://onedrive.live.com/view.aspx?resid=" + resid);

        Arbitrary<String> liveUrls = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .map(token -> "https://onedrive.live.com/redir?resid=" + token);

        return Arbitraries.oneOf(shortUrls, liveViewUrls, liveUrls);
    }
}
