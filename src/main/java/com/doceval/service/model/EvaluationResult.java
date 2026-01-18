package com.doceval.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Evaluation result
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {
    
    private String documentId;
    private ApprovalStatus approvalStatus;
    private String reason;
    private Double confidenceScore;
    private LocalDateTime evaluatedAt;
    
    // Detailed rule check results
    private List<RuleCheckResult> ruleChecks;
    
    // Additional context from RAG retrieval
    private List<String> relevantContext;
    
    // LLM interaction metadata
    private Map<String, Object> llmMetadata;
    
    public enum ApprovalStatus {
        APPROVED,
        REJECTED,
        NEEDS_REVIEW
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleCheckResult {
        private String ruleName;
        private Boolean passed;
        private String details;
        private Double confidence;
    }
}
