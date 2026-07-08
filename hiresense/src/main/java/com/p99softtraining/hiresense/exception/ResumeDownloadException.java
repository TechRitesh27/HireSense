package com.p99softtraining.hiresense.exception;

public class ResumeDownloadException extends RuntimeException {

    public ResumeDownloadException(String message) {
        super(message);
    }

    public ResumeDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
