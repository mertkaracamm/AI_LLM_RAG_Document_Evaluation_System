package com.doceval.service.agent;

import com.doceval.service.model.Document;
import com.doceval.service.model.EvaluationResult;
import com.doceval.service.model.EvaluationRule;
import com.doceval.service.service.LlmService;
import com.doceval.service.service.VectorSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

// Agent workflow orchestration
@Slf4j
@Component
public class EvaluationAgent {
    
    private final LlmService llmService;
    private final VectorSearchService vectorSearchService;
    private final RuleEngine ruleEngine;
    
    public EvaluationAgent(
            LlmService llmService,
            VectorSearchService vectorSearchService,
            RuleEngine ruleEngine) {
        this.llmService = llmService;
        this.vectorSearchService = vectorSearchService;
        this.ruleEngine = ruleEngine;
    }
    
    // TODO: Add description
    public EvaluationResult evaluateDocument(Document document) {
        log.info("Starting agent evaluation for document: {}", document.getId());
        
        AgentState state = new AgentState(document.getId());
        
        try {
            // Step 1: Planning - determine what to check
            List<EvaluationRule> applicableRules = planEvaluation(document);
            state.addStep("Planning complete: " + applicableRules.size() + " rules identified");
            
            // Step 2: Context retrieval using RAG
            List<String> relevantContext = retrieveContext(document);
            state.addStep("Context retrieved: " + relevantContext.size() + " similar documents");
            
            // Step 3: Execute rule checks
            List<String> ruleDescriptions = applicableRules.stream()
                .map(EvaluationRule::getDescription)
                .toList();
            
            EvaluationResult result = llmService.evaluateDocument(
                document.getContent(), 
                ruleDescriptions
            );
            state.addStep("LLM evaluation complete");
            
            // Step 4: Enhance result with context
            result.setDocumentId(document.getId());
            result.setRelevantContext(relevantContext);
            result.setLlmMetadata(state.toMetadata());
            
            // Step 5: Validate and adjust confidence based on context
            adjustConfidenceWithContext(result, relevantContext);
            state.addStep("Confidence adjustment complete");
            
            log.info("Evaluation complete for {}: {} (confidence: {})", 
                document.getId(), 
                result.getApprovalStatus(),
                result.getConfidenceScore());
            
            return result;
            
        } catch (Exception e) {
            log.error("Agent evaluation failed for document: {}", document.getId(), e);
            state.addStep("Error: " + e.getMessage());
            
            // Return failed result with error details - include empty ruleChecks
            return EvaluationResult.builder()
                .documentId(document.getId())
                .approvalStatus(EvaluationResult.ApprovalStatus.NEEDS_REVIEW)
                .reason("Evaluation failed: " + e.getMessage())
                .confidenceScore(0.0)
                .evaluatedAt(LocalDateTime.now())
                .ruleChecks(new ArrayList<>())  // Initialize as empty list
                .llmMetadata(state.toMetadata())
                .build();
        }
    }
    
   //Determines which rules apply to this document type.
    private List<EvaluationRule> planEvaluation(Document document) {
        // Get rules from rule engine based on document type
        List<EvaluationRule> allRules = ruleEngine.getAllRules();
        
        // Filter rules based on document metadata
        String docType = document.getMetadata() != null ? 
            document.getMetadata().getDocumentType() : "GENERAL";
        
        // For now, return all rules. Can be extended with smart filtering
        log.debug("Planning evaluation: {} rules applicable for type {}", 
            allRules.size(), docType);
        
        return allRules;
    }
    
   //Retrieves relevant context using RAG (Retrieval-Augmented Generation).
    private List<String> retrieveContext(Document document) {
        // Extract key phrases for context search
        String searchQuery = extractKeyPhrases(document.getContent());
        
        // Perform semantic search
        return vectorSearchService.getRelevantContext(searchQuery, 3);
    }
    
    //Extracts important phrases from document for better context retrieval.
    private String extractKeyPhrases(String content) {
        // Take first 200 words as representative sample
        String[] words = content.split("\\s+");
        int limit = Math.min(200, words.length);
        return String.join(" ", Arrays.copyOfRange(words, 0, limit));
    }
    
    //Adjusts confidence score based on retrieved context quality.
    private void adjustConfidenceWithContext(
            EvaluationResult result, 
            List<String> context) {
        
        if (context.isEmpty()) {
            // No context found - reduce confidence slightly
            double adjusted = result.getConfidenceScore() * 0.95;
            result.setConfidenceScore(adjusted);
            return;
        }
        
        // With good context, we can be more confident
        double boost = Math.min(0.1, context.size() * 0.03);
        double adjusted = Math.min(1.0, result.getConfidenceScore() + boost);
        result.setConfidenceScore(adjusted);
    }
    
   //Internal state tracker for agent workflow.
    private static class AgentState {
        private final String documentId;
        private final List<String> executionSteps;
        private final LocalDateTime startTime;
        
        public AgentState(String documentId) {
            this.documentId = documentId;
            this.executionSteps = new ArrayList<>();
            this.startTime = LocalDateTime.now();
        }
        
        public void addStep(String step) {
            executionSteps.add(String.format("[%s] %s", 
                LocalDateTime.now(), step));
        }
        
        public Map<String, Object> toMetadata() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", documentId);
            metadata.put("start_time", startTime.toString());
            metadata.put("execution_steps", executionSteps);
            metadata.put("total_steps", executionSteps.size());
            return metadata;
        }
    }
}
