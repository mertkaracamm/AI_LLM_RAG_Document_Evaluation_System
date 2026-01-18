package com.doceval.service.dto;

import com.doceval.service.model.EvaluationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {
    
    private String documentId;
    private String status;
    private String decision;
    private String reasoning;
    private Double confidence;
    private LocalDateTime timestamp;
    private List<RuleCheckSummary> ruleResults;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleCheckSummary {
        private String rule;
        private Boolean passed;
        private String explanation;
    }
    
    public static EvaluationResponse fromEvaluationResult(EvaluationResult result) {
        // Handle null ruleChecks safely
        List<RuleCheckSummary> summaries = new ArrayList<>();
        if (result.getRuleChecks() != null) {
            summaries = result.getRuleChecks().stream()
                .map(rc -> RuleCheckSummary.builder()
                    .rule(rc.getRuleName())
                    .passed(rc.getPassed())
                    .explanation(rc.getDetails())
                    .build())
                .toList();
        }
            
        return EvaluationResponse.builder()
            .documentId(result.getDocumentId())
            .status("COMPLETED")
            .decision(result.getApprovalStatus() != null ? result.getApprovalStatus().toString() : "UNKNOWN")
            .reasoning(result.getReason())
            .confidence(result.getConfidenceScore())
            .timestamp(result.getEvaluatedAt())
            .ruleResults(summaries)
            .build();
    }
}
