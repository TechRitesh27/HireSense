package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.exception.ResumeTextExtractionException;

/**
 * Strategy interface for extracting plain text from a downloaded resume binary.
 * Multiple implementations handle different document formats (PDF, DOCX).
 * The service layer iterates the injected List&lt;ResumeTextExtractor&gt; and uses the first
 * implementation where supports(bytes) returns true.
 *
 * <p>Supports requirements 2.1 and 2.2 — PDF and DOCX text extraction.</p>
 */
public interface ResumeTextExtractor {

    /**
     * Returns true if this extractor can handle the given byte array based on magic bytes.
     *
     * @param bytes the raw document bytes; may be null or empty
     */
    boolean supports(byte[] bytes);

    /**
     * Extracts all plain text from the given document bytes.
     *
     * @param bytes the raw document bytes
     * @return the extracted text (non-blank, at least 50 characters)
     * @throws ResumeTextExtractionException if the text is blank, too short, or extraction fails
     */
    String extract(byte[] bytes) throws ResumeTextExtractionException;
}
