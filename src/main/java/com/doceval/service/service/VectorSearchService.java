package com.doceval.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.doceval.service.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

// Vector search with Redis
@Slf4j
@Service
public class VectorSearchService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LlmService llmService;
    
    private static final String DOCUMENT_PREFIX = "doc:";
    private static final String EMBEDDING_PREFIX = "emb:";
    
    public VectorSearchService(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            LlmService llmService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.llmService = llmService;
        
        log.info("VectorSearchService initialized");
        log.info("RedisTemplate instance: {}", redisTemplate);
        log.info("RedisTemplate class: {}", redisTemplate.getClass());
    }
    
    //Stores document with its embedding vector in Redis.
    public void indexDocument(Document document) {
        log.debug("Indexing document: {}", document.getId());

        try {
            // Generate embedding if not present
            if (document.getEmbedding() == null || document.getEmbedding().isEmpty()) {
                log.info("Generating embedding for document: {}", document.getId());
                List<Float> embedding = llmService.generateEmbedding(document.getContent());
                document.setEmbedding(embedding);
                log.info("Embedding generated, size: {}", embedding.size());
            }

            // Store document
            String docKey = DOCUMENT_PREFIX + document.getId();
            log.info("Storing document with key: {}", docKey);
            redisTemplate.opsForValue().set(docKey, document);
            log.info("Document stored successfully");

            // Store embedding separately for vector search
            String embKey = EMBEDDING_PREFIX + document.getId();
            log.info("Storing embedding with key: {}", embKey);
            redisTemplate.opsForValue().set(embKey, document.getEmbedding());
            log.info("Embedding stored successfully");

            log.info("Document indexed successfully: {}", document.getId());

        } catch (Exception e) {
            log.error("Failed to index document: {}", document.getId(), e);
            e.printStackTrace();
            throw new RuntimeException("Document indexing failed", e);
        }
    }
    
    // Performs semantic search using cosine similarity
 // Performs semantic search using cosine similarity
    public List<SimilarityResult> semanticSearch(String queryText, int topK) {
        log.debug("Performing semantic search for: {}", queryText);
        
        try {
            // Generate query embedding
            List<Float> queryEmbedding = llmService.generateEmbedding(queryText);
            log.info("Query embedding generated, size: {}", queryEmbedding.size());
            
            // Get all document embeddings
            Set<String> embeddingKeys = redisTemplate.keys(EMBEDDING_PREFIX + "*");
            
            if (embeddingKeys == null || embeddingKeys.isEmpty()) {
                log.warn("No indexed documents found for search");
                return Collections.emptyList();
            }
            
            log.info("Found {} embedding keys to search", embeddingKeys.size());
            
            // Calculate similarity scores
            List<SimilarityResult> results = new ArrayList<>();
            
            for (String embKey : embeddingKeys) {
                String docId = embKey.replace(EMBEDDING_PREFIX, "");
                Object embObj = redisTemplate.opsForValue().get(embKey);
                
                if (embObj != null) {
                    log.debug("Processing embedding for doc: {}, type: {}", docId, embObj.getClass().getName());
                    
                    // Convert to List<Float> - handle multiple types
                    List<Float> docEmbedding = convertToFloatList(embObj);
                    
                    if (!docEmbedding.isEmpty() && docEmbedding.size() == queryEmbedding.size()) {
                        double similarity = cosineSimilarity(queryEmbedding, docEmbedding);
                        log.debug("Similarity for {}: {}", docId, similarity);
                        
                        // Retrieve document from Redis
                        Object docObj = redisTemplate.opsForValue().get(DOCUMENT_PREFIX + docId);
                        if (docObj != null) {
                            Document doc = convertToDocument(docObj);
                            if (doc != null) {
                                results.add(new SimilarityResult(doc, similarity));
                            }
                        }
                    } else {
                        log.warn("Embedding size mismatch for {}: {} vs {}", 
                            docId, docEmbedding.size(), queryEmbedding.size());
                    }
                }
            }
            
            log.info("Found {} results", results.size());
            
            // Sort by similarity and return top K
            return results.stream()
                .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                .limit(topK)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Semantic search failed", e);
            e.printStackTrace();
            throw new RuntimeException("Search operation failed", e);
        }
    }
    
    /**
     * Safely converts Redis object to List<Float>
     */
    private List<Float> convertToFloatList(Object obj) {
        List<Float> result = new ArrayList<>();
        
        try {
            if (obj instanceof List) {
                for (Object item : (List<?>) obj) {
                    if (item instanceof Number) {
                        result.add(((Number) item).floatValue());
                    } else if (item instanceof String) {
                        result.add(Float.parseFloat((String) item));
                    }
                }
            } else if (obj instanceof String) {
                // Handle JSON string
                String jsonStr = (String) obj;
                jsonStr = jsonStr.trim();
                if (jsonStr.startsWith("[") && jsonStr.endsWith("]")) {
                    jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
                    String[] parts = jsonStr.split(",");
                    for (String part : parts) {
                        result.add(Float.parseFloat(part.trim()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to convert object to float list: {}", obj.getClass(), e);
        }
        
        return result;
    }
    
    /**
     * Safely converts Redis object to Document
     */
    private Document convertToDocument(Object obj) {
        try {
            if (obj instanceof Document) {
                return (Document) obj;
            } else if (obj instanceof Map) {
                // Convert Map to Document using ObjectMapper
                return objectMapper.convertValue(obj, Document.class);
            } else {
                log.warn("Unexpected document type: {}", obj.getClass());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to convert object to Document", e);
            return null;
        }
    }
    
   //Retrieves relevant context for RAG.
    public List<String> getRelevantContext(String query, int maxResults) {
        List<SimilarityResult> results = semanticSearch(query, maxResults);
        
        return results.stream()
            .filter(r -> r.getSimilarity() > 0.3) // Only high-similarity results
            .map(r -> r.getDocument().getContent())
            .collect(Collectors.toList());
    }
    
  //Calculates cosine similarity between two embedding vectors.
    private double cosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("Vectors must have same dimension");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
   
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SimilarityResult {
        private Document document;
        private Double similarity;
    }
}