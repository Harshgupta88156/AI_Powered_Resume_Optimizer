package com.ai_resume.resume_service.service;

import com.ai_resume.resume_service.exception.FileProcessingException;
import com.ai_resume.resume_service.exception.InvalidFileTypeException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DocumentTextExtractor {

    /**
     * Extracts plain text from a PDF, DOC, or DOCX file given its raw bytes.
     * Accepting {@code byte[]} (rather than {@link org.springframework.web.multipart.MultipartFile})
     * prevents the "stream already consumed" bug that occurs when the same MultipartFile is read
     * by both Cloudinary upload and text extraction in the same request.
     *
     * @param fileBytes   raw bytes of the uploaded file
     * @param filename    original file name (used for extension-based type detection fallback)
     * @param contentType MIME type reported by the client
     * @return extracted plain text, never blank
     * @throws InvalidFileTypeException if the file type is not supported
     * @throws FileProcessingException  if the file cannot be parsed or contains no readable text
     */
    public String extractText(byte[] fileBytes, String filename, String contentType) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new FileProcessingException("Cannot extract text: file is empty – " + filename, null);
        }

        String lowerName = filename == null ? "" : filename.toLowerCase().trim();
        String lowerType = contentType == null ? "" : contentType.toLowerCase().trim();

        log.info("Starting text extraction for '{}' ({} bytes, contentType={})", filename, fileBytes.length, contentType);

        try {
            if (isPdf(lowerType, lowerName)) {
                return extractFromPdf(fileBytes, filename);
            } else if (isDocx(lowerType, lowerName)) {
                return extractFromDocx(fileBytes, filename);
            } else if (isDoc(lowerType, lowerName)) {
                return extractFromDoc(fileBytes, filename);
            } else {
                throw new InvalidFileTypeException(
                        "Unsupported file type for text extraction: '" + filename + "' (contentType: " + contentType + ")");
            }
        } catch (InvalidFileTypeException | FileProcessingException ex) {
            // Already a meaningful, correctly-mapped exception (400 / 500).
            // Re-wrapping these was turning every "unsupported type" into a 500.
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while extracting text from '{}': {}", filename, ex.getMessage(), ex);
            throw new FileProcessingException("Failed to extract text from document: " + filename, ex);
        }
    }

    // -------------------------------------------------------------------------
    // PDF (Apache PDFBox 3.x)
    // -------------------------------------------------------------------------

    private String extractFromPdf(byte[] bytes, String filename) {
        log.debug("Extracting text from PDF '{}'", filename);
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                // Attempt to open with an empty owner/user password (common for "print-protected" PDFs)
                log.warn("PDF '{}' is encrypted – attempting empty-password decryption", filename);
                try {
                    document.setAllSecurityToBeRemoved(true);
                } catch (Exception decryptEx) {
                    log.error("Could not remove encryption from '{}': {}", filename, decryptEx.getMessage());
                    throw new FileProcessingException(
                            "PDF is password-protected and cannot be read: " + filename, decryptEx);
                }
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            return validated(text, filename);
        } catch (IOException ex) {
            log.error("IOException parsing PDF '{}': {}", filename, ex.getMessage(), ex);
            throw new FileProcessingException("Failed to parse PDF document: " + filename, ex);
        }
    }

    // -------------------------------------------------------------------------
    // DOCX (Apache POI – XWPF)
    // -------------------------------------------------------------------------

    private String extractFromDocx(byte[] bytes, String filename) {
        log.debug("Extracting text from DOCX '{}'", filename);
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            return validated(text, filename);
        } catch (IOException ex) {
            log.error("IOException parsing DOCX '{}': {}", filename, ex.getMessage(), ex);
            throw new FileProcessingException("Failed to parse DOCX document: " + filename, ex);
        }
    }

    // -------------------------------------------------------------------------
    // DOC (Apache POI – HWPF, requires poi-scratchpad)
    // -------------------------------------------------------------------------

    private String extractFromDoc(byte[] bytes, String filename) {
        log.debug("Extracting text from DOC '{}'", filename);
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(document)) {
            String text = extractor.getText();
            return validated(text, filename);
        } catch (IOException ex) {
            log.error("IOException parsing DOC '{}': {}", filename, ex.getMessage(), ex);
            throw new FileProcessingException("Failed to parse DOC document: " + filename, ex);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String validated(String text, String filename) {
        if (text == null || text.isBlank()) {
            log.warn("No readable text found in '{}'", filename);
            throw new FileProcessingException("No readable text found in document: " + filename, null);
        }
        String trimmed = text.trim();
        log.info("Extracted {} characters of text from '{}'", trimmed.length(), filename);
        return trimmed;
    }

    private boolean isPdf(String contentType, String lowerName) {
        return contentType.contains("application/pdf") || lowerName.endsWith(".pdf");
    }

    private boolean isDocx(String contentType, String lowerName) {
        return contentType.contains("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || lowerName.endsWith(".docx");
    }

    private boolean isDoc(String contentType, String lowerName) {
        return contentType.contains("application/msword") || lowerName.endsWith(".doc");
    }
}
