package com.p99softtraining.hiresense.service.impl;

import com.p99softtraining.hiresense.exception.ResumeTextExtractionException;
import com.p99softtraining.hiresense.service.ResumeTextExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Extracts plain text from DOCX documents using Apache POI 5.2.5 {@link XWPFDocument}.
 *
 * <p>Detection is based on the ZIP/PK magic bytes (0x50 0x4B), which are the common
 * header for all Office Open XML formats including .docx files.</p>
 *
 * <p>Both paragraph text and table cell text are extracted. Newlines are preserved
 * between paragraphs and table rows to retain structural context for the LLM prompt.</p>
 *
 * <p>Supports requirements 2.2, 2.4, and 2.5.</p>
 */
@Component
public class DocxTextExtractor implements ResumeTextExtractor {

    /** ZIP/PK magic bytes (covers DOCX, which is a ZIP-based format) */
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B};

    private static final int MIN_TEXT_LENGTH = 50;

    @Override
    public boolean supports(byte[] bytes) {
        if (bytes == null || bytes.length < ZIP_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < ZIP_MAGIC.length; i++) {
            if (bytes[i] != ZIP_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String extract(byte[] bytes) throws ResumeTextExtractionException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder fullText = new StringBuilder();

            // Extract paragraph text, preserving newlines between paragraphs
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String paraText = paragraph.getText();
                if (paraText != null && !paraText.isBlank()) {
                    fullText.append(paraText).append("\n");
                }
            }

            // Extract table cell text, preserving newlines between rows and cells
            List<XWPFTable> tables = document.getTables();
            for (XWPFTable table : tables) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.isBlank()) {
                            fullText.append(cellText).append("\t");
                        }
                    }
                    fullText.append("\n");
                }
            }

            String text = fullText.toString();
            if (text.isBlank() || text.strip().length() < MIN_TEXT_LENGTH) {
                throw new ResumeTextExtractionException(
                        "DOCX appears to be empty or contains no readable text: extracted text has fewer than "
                                + MIN_TEXT_LENGTH + " characters."
                );
            }
            return text;

        } catch (ResumeTextExtractionException e) {
            throw e;
        } catch (IOException e) {
            throw new ResumeTextExtractionException(
                    "Failed to extract text from DOCX document: " + e.getMessage(), e
            );
        }
    }
}
