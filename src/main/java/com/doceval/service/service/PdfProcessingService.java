package com.doceval.service.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;


@Slf4j
@Service
public class PdfProcessingService {
    
    // TODO: Add description
    public String extractText(byte[] pdfBytes) throws IOException {
        log.debug("Starting PDF text extraction, size: {} bytes", pdfBytes.length);
        
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            
            if (document.isEncrypted()) {
                log.warn("PDF is encrypted, attempting to decrypt");
                document.setAllSecurityToBeRemoved(true);
            }
            
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            
            String text = stripper.getText(document);
            
            log.info("Successfully extracted {} characters from {} pages", 
                text.length(), document.getNumberOfPages());
            
            return text;
            
        } catch (IOException e) {
            log.error("Failed to extract text from PDF", e);
            throw new IOException("PDF text extraction failed: " + e.getMessage(), e);
        }
    }
    
    //Extracts metadata from PDF document.
    public PdfMetadata extractMetadata(byte[] pdfBytes) throws IOException {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            
            var info = document.getDocumentInformation();
            
            return PdfMetadata.builder()
                .pageCount(document.getNumberOfPages())
                .title(info.getTitle())
                .author(info.getAuthor())
                .subject(info.getSubject())
                .keywords(info.getKeywords())
                .creationDate(info.getCreationDate() != null ? 
                    info.getCreationDate().toInstant().toString() : null)
                .build();
                
        } catch (IOException e) {
            log.error("Failed to extract PDF metadata", e);
            throw new IOException("PDF metadata extraction failed: " + e.getMessage(), e);
        }
    }
    
    //Simple POJO for PDF metadata
    @lombok.Data
    @lombok.Builder
    public static class PdfMetadata {
        private Integer pageCount;
        private String title;
        private String author;
        private String subject;
        private String keywords;
        private String creationDate;
    }
}
