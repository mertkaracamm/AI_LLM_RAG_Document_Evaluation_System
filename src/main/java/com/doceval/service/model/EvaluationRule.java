package com.doceval.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Compliance rule
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRule {
    
    private String id;
    private String name;
    private String description;
    private RuleType type;
    private String condition;
    private Integer priority;
    private Double weight;
    private Boolean mandatory;
    
    public enum RuleType {
        KEYWORD_PRESENCE,      // Must contain specific keywords
        SIGNATURE_CHECK,       // Must have signature section
        DATE_VALIDATION,       // Date must be within range
        SEMANTIC_MATCH,        // Semantic similarity check using RAG
        CUSTOM_LLM             // Custom LLM-based validation
    }
}
