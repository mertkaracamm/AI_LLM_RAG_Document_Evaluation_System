package com.doceval.service.service;

import com.doceval.service.model.EvaluationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LLM Service.
 * 
 * Note: These tests require a valid OpenAI API key in application.properties
 * For CI/CD, consider mocking the OpenAI service.
 */
@SpringBootTest
class LlmServiceTest {
    
    @Autowired(required = false)
    private LlmService llmService;
    
    @Test
    void testEmbeddingGeneration() {
        // Skip if service not available (missing API key)
        if (llmService == null) {
            return;
        }
        
        String text = "This is a test document about financial compliance.";
        List<Float> embedding = llmService.generateEmbedding(text);
        
        assertNotNull(embedding);
        assertEquals(1536, embedding.size()); // text-embedding-3-small dimension
    }
    
    @Test
    void testDocumentEvaluation() {
        // Skip if service not available
        if (llmService == null) {
            return;
        }
        
        String documentContent = """
            APPROVAL LETTER
            
            Date: January 15, 2024
            
            This letter confirms that the proposal has been approved by both parties.
            
            Signed: John Doe
            Authorized Representative
            """;
        
        List<String> rules = Arrays.asList(
            "Document must contain approval language",
            "Document must have a signature",
            "Document must have a date"
        );
        
        EvaluationResult result = llmService.evaluateDocument(documentContent, rules);
        
        assertNotNull(result);
        assertNotNull(result.getApprovalStatus());
        assertNotNull(result.getReason());
        assertTrue(result.getConfidenceScore() >= 0 && result.getConfidenceScore() <= 1);
        assertFalse(result.getRuleChecks().isEmpty());
    }
}
