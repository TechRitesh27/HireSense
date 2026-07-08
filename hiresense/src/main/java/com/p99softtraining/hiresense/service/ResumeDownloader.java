package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.exception.ResumeDownloadException;

/**
 * Strategy interface for downloading a resume binary from a URL.
 * Multiple implementations handle different URL types (Google Drive, OneDrive, direct HTTPS).
 * The service layer iterates the injected List<ResumeDownloader> and uses the first
 * implementation where supports(url) returns true.
 */
public interface ResumeDownloader {

    /**
     * Returns true if this downloader can handle the given URL.
     */
    boolean supports(String url);

    /**
     * Downloads the resume binary from the given URL and returns it as a byte array.
     *
     * @throws ResumeDownloadException if the download fails (non-200 status, empty body, timeout)
     */
    byte[] download(String url) throws ResumeDownloadException;
}
