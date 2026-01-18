package com.doceval.service.controller;

import com.doceval.service.dto.EvaluationResponse;
import com.doceval.service.model.Document;
import com.doceval.service.model.EvaluationResult;
import com.doceval.service.service.DocumentService;
import com.doceval.service.service.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// REST API endpoints
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Evaluation", description = "LLM-based document compliance evaluation API")
public class DocumentController {
    
    private final DocumentService documentService;
    
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }
    
   //Upload a PDF document for evaluation.
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document", 
               description = "Uploads a PDF document and extracts its content for evaluation")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        
        log.info("Received document upload request: {}", file.getOriginalFilename());
        
        try {
            Document document = documentService.uploadDocument(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "message", "Document uploaded successfully",
                "documentId", document.getId(),
                "filename", document.getFilename(),
                "wordCount", document.getMetadata().getWordCount(),
                "pageCount", document.getMetadata().getPageCount()
            ));
            
        } catch (IOException e) {
            log.error("Failed to process document upload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "error",
                "message", "Failed to process document: " + e.getMessage()
            ));
        }
    }
    
   //Trigger evaluation for an uploaded document.
    @PostMapping("/{documentId}/evaluate")
    @Operation(summary = "Evaluate a document", 
               description = "Runs LLM-based compliance evaluation on the document")
    public ResponseEntity<EvaluationResponse> evaluateDocument(
            @PathVariable String documentId) {
        
        log.info("Received evaluation request for document: {}", documentId);
        
        try {
            EvaluationResult result = documentService.evaluateDocument(documentId);
            EvaluationResponse response = EvaluationResponse.fromEvaluationResult(result);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Document not found: {}", documentId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Evaluation failed for document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(EvaluationResponse.builder()
                    .status("ERROR")
                    .decision("FAILED")
                    .reasoning("Evaluation failed: " + e.getMessage())
                    .build());
        }
    }
    
   //Get document details.
    @GetMapping("/{documentId}")
    @Operation(summary = "Get document", description = "Retrieves document information")
    public ResponseEntity<Document> getDocument(@PathVariable String documentId) {
        Document document = documentService.getDocument(documentId);
        
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(document);
    }
    
    //Get evaluation result for a document.
    @GetMapping("/{documentId}/result")
    @Operation(summary = "Get evaluation result", 
               description = "Retrieves the evaluation result for a document")
    public ResponseEntity<EvaluationResponse> getEvaluationResult(
            @PathVariable String documentId) {
        
        EvaluationResult result = documentService.getEvaluationResult(documentId);
        
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(EvaluationResponse.fromEvaluationResult(result));
    }
    
    
    @GetMapping
    @Operation(summary = "List documents", description = "Lists all uploaded documents")
    public ResponseEntity<List<Document>> listDocuments() {
        List<Document> documents = documentService.listDocuments();
        return ResponseEntity.ok(documents);
    }
    
    //Search documents using semantic similarity.
    @GetMapping("/search")
    @Operation(summary = "Search documents", 
               description = "Semantic search across document content using embeddings")
    public ResponseEntity<List<Map<String, Object>>> searchDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        
        log.info("Performing semantic search: {}", query);
        
        List<VectorSearchService.SimilarityResult> results = 
            documentService.searchDocuments(query, limit);
        
        List<Map<String, Object>> response = new java.util.ArrayList<>();
        for (VectorSearchService.SimilarityResult r : results) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("documentId", r.getDocument().getId());
            item.put("filename", r.getDocument().getFilename());
            item.put("similarity", r.getSimilarity());
            item.put("excerpt", r.getDocument().getContent().substring(
                0, Math.min(200, r.getDocument().getContent().length())) + "...");
            response.add(item);
        }
        
        return ResponseEntity.ok(response);
    }
    
    
    @PostMapping(value = "/upload-and-evaluate", 
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and evaluate", 
               description = "Uploads a document and immediately evaluates it")
    public ResponseEntity<EvaluationResponse> uploadAndEvaluate(
            @RequestParam("file") MultipartFile file) {
        
        log.info("Received upload-and-evaluate request: {}", file.getOriginalFilename());
        
        try {
            // Upload
            Document document = documentService.uploadDocument(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
            );
            
            // Evaluate
            EvaluationResult result = documentService.evaluateDocument(document.getId());
            
            return ResponseEntity.ok(EvaluationResponse.fromEvaluationResult(result));
            
        } catch (Exception e) {
            log.error("Upload and evaluate failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(EvaluationResponse.builder()
                    .status("ERROR")
                    .reasoning("Operation failed: " + e.getMessage())
                    .build());
        }
    }
}
