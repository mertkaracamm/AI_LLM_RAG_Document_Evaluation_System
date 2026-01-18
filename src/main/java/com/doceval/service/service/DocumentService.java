package com.doceval.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.doceval.service.agent.EvaluationAgent;
import com.doceval.service.model.Document;
import com.doceval.service.model.DocumentMetadata;
import com.doceval.service.model.EvaluationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

// Document service
@Slf4j
@Service
public class DocumentService {
    
    private final PdfProcessingService pdfService;
    private final VectorSearchService vectorSearchService;
    private final EvaluationAgent evaluationAgent;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String DOC_KEY_PREFIX = "doc:";
    private static final String RESULT_KEY_PREFIX = "result:";
    
    public DocumentService(
            PdfProcessingService pdfService,
            VectorSearchService vectorSearchService,
            EvaluationAgent evaluationAgent,
            RedisTemplate<String, Object> redisTemplate) {
        this.pdfService = pdfService;
        this.vectorSearchService = vectorSearchService;
        this.evaluationAgent = evaluationAgent;
        this.redisTemplate = redisTemplate;
    }
    
    // TODO: Add description
    public Document uploadDocument(
            String filename, 
            String contentType, 
            byte[] fileContent) throws IOException {
        
        log.info("Processing new document upload: {}", filename);
        
        // Generate unique ID
        String docId = UUID.randomUUID().toString();
        
        // Extract text from PDF
        String extractedText = pdfService.extractText(fileContent);
        PdfProcessingService.PdfMetadata pdfMeta = pdfService.extractMetadata(fileContent);
        
        // Build document metadata
        DocumentMetadata metadata = DocumentMetadata.builder()
            .documentType("UNKNOWN") // Can be enhanced with classification
            .pageCount(pdfMeta.getPageCount())
            .wordCount((long) extractedText.split("\\s+").length)
            .extractedFields(new HashMap<>())
            .confidenceScores(new HashMap<>())
            .build();
        
        // Create document object
        Document document = Document.builder()
            .id(docId)
            .filename(filename)
            .contentType(contentType)
            .content(extractedText)
            .status(Document.DocumentStatus.UPLOADED)
            .uploadedAt(LocalDateTime.now())
            .metadata(metadata)
            .build();
        
        // Index document for vector search
        vectorSearchService.indexDocument(document);
        
        // Store in Redis
        redisTemplate.opsForValue().set(DOC_KEY_PREFIX + docId, document);
        
        log.info("Document uploaded successfully: {}", docId);
        return document;
    }
    
    //Evaluates a document against compliance rules.
    public EvaluationResult evaluateDocument(String documentId) {
        log.info("Starting evaluation for document: {}", documentId);
        
        // Retrieve document
        Document document = getDocument(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }
        
        // Update status
        document.setStatus(Document.DocumentStatus.PROCESSING);
        redisTemplate.opsForValue().set(DOC_KEY_PREFIX + documentId, document);
        
        try {
            // Run agent-based evaluation
            EvaluationResult result = evaluationAgent.evaluateDocument(document);
            
            // Update document status
            document.setStatus(Document.DocumentStatus.EVALUATED);
            document.setEvaluatedAt(LocalDateTime.now());
            redisTemplate.opsForValue().set(DOC_KEY_PREFIX + documentId, document);
            
            // Store evaluation result
            redisTemplate.opsForValue().set(RESULT_KEY_PREFIX + documentId, result);
            
            log.info("Evaluation completed for {}: {}", 
                documentId, result.getApprovalStatus());
            
            return result;
            
        } catch (Exception e) {
            log.error("Evaluation failed for document: {}", documentId, e);
            document.setStatus(Document.DocumentStatus.FAILED);
            redisTemplate.opsForValue().set(DOC_KEY_PREFIX + documentId, document);
            throw new RuntimeException("Evaluation failed", e);
        }
    }
    
    // Retrieves a document by ID.
    public Document getDocument(String documentId) {
        Object obj = redisTemplate.opsForValue().get(DOC_KEY_PREFIX + documentId);
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof Document) {
            return (Document) obj;
        }
        
        // Convert LinkedHashMap to Document
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return mapper.convertValue(obj, Document.class);
    }
    
  //Retrieves evaluation result for a document.
    public EvaluationResult getEvaluationResult(String documentId) {
        Object obj = redisTemplate.opsForValue().get(RESULT_KEY_PREFIX + documentId);
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof EvaluationResult) {
            return (EvaluationResult) obj;
        }
        
        // Convert LinkedHashMap to EvaluationResult
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return mapper.convertValue(obj, EvaluationResult.class);
    }
    
    
    public List<Document> listDocuments() {
        Set<String> keys = redisTemplate.keys(DOC_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        
        return keys.stream()
            .map(key -> {
                Object obj = redisTemplate.opsForValue().get(key);
                if (obj == null) return null;
                if (obj instanceof Document) return (Document) obj;
                return mapper.convertValue(obj, Document.class);
            })
            .filter(Objects::nonNull)
            .toList();
    }
    
   
    public List<VectorSearchService.SimilarityResult> searchDocuments(
            String query, 
            int limit) {
        return vectorSearchService.semanticSearch(query, limit);
    }
}
