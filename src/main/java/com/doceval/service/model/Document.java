package com.doceval.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// Document model
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    
    private String id;
    private String filename;
    private String contentType;
    private String content;
    private DocumentStatus status;
    private LocalDateTime uploadedAt;
    private LocalDateTime evaluatedAt;
    
    // Vector embedding for semantic search
    private List<Float> embedding;
    
    // Metadata extracted during processing
    private DocumentMetadata metadata;
    
    public enum DocumentStatus {
        UPLOADED,
        PROCESSING,
        EVALUATED,
        FAILED
    }
}
