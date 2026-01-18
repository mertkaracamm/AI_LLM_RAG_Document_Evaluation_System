package com.doceval.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadata {
    
    private String documentType;
    private Integer pageCount;
    private Long wordCount;
    
    // Extracted entities (dates, names, amounts, etc.)
    private Map<String, String> extractedFields;
    
    // Confidence scores for extracted information
    private Map<String, Double> confidenceScores;
}
