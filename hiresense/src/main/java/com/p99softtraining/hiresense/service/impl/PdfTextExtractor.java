package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.exception.ResumeTextExtractionException;
import com.p99softtraining.hiresense.service.ResumeTextExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Extracts plain text from PDF documents using Apache PDFBox 3.0.3.
 *
 * <p>Detection is based on magic bytes {@code %PDF} (0x25 0x50 0x44 0x46).
 * Newlines between pages are preserved to retain structural context for the LLM prompt.</p>
 *
 * <p>Supports requirements 2.1, 2.4, and 2.5.</p>
 */
@Component
public class PdfTextExtractor implements ResumeTextExtractor {

    /** PDF magic bytes: %PDF */
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};

    private static final int MIN_TEXT_LENGTH = 50;

    @Override
    public boolean supports(byte[] bytes) {
        if (bytes == null || bytes.length < PDF_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (bytes[i] != PDF_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String extract(byte[] bytes) throws ResumeTextExtractionException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            // Preserve newlines between pages for structural context
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(false);

            StringBuilder fullText = new StringBuilder();
            int pageCount = document.getNumberOfPages();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                if (pageText != null && !pageText.isBlank()) {
                    fullText.append(pageText);
                    if (page < pageCount) {
                        fullText.append("\n");
                    }
                }
            }

            String text = fullText.toString();
            if (text.isBlank() || text.strip().length() < MIN_TEXT_LENGTH) {
                throw new ResumeTextExtractionException(
                        "PDF appears to be empty or image-only: extracted text has fewer than "
                                + MIN_TEXT_LENGTH + " characters."
                );
            }
            return text;

        } catch (ResumeTextExtractionException e) {
            throw e;
        } catch (IOException e) {
            throw new ResumeTextExtractionException(
                    "Failed to extract text from PDF document: " + e.getMessage(), e
            );
        }
    }
}
