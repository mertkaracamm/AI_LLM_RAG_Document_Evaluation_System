package com.doceval.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.doceval.service.model.EvaluationResult;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

// OpenAI service integration
@Slf4j
@Service
public class LlmService {
    
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    
    public LlmService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.model}") String model,
            @Value("${openai.temperature}") Double temperature,
            @Value("${openai.max-tokens}") Integer maxTokens) {
        
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(60));
        this.objectMapper = new ObjectMapper();
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        
        log.info("LLM Service initialized with model: {}", model);
    }
    
    // TODO: Add description
    public EvaluationResult evaluateDocument(String documentContent, List<String> rules) {
        log.debug("Starting LLM evaluation for document with {} rules", rules.size());
        
        String prompt = buildEvaluationPrompt(documentContent, rules);
        String response = callLlm(prompt);
        
        return parseEvaluationResponse(response);
    }
    
   //Generates embedding vector for text content.
    public List<Float> generateEmbedding(String text) {
        try {
            EmbeddingRequest request = EmbeddingRequest.builder()
                .model("text-embedding-3-small")
                .input(Collections.singletonList(text))
                .build();
            
            var result = openAiService.createEmbeddings(request);
            
            // Convert Double to Float for Redis vector storage
            return result.getData().get(0).getEmbedding().stream()
                .map(Double::floatValue)
                .toList();
                
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }
    
   //Builds a structured prompt for document evaluation.
    private String buildEvaluationPrompt(String content, List<String> rules) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a document compliance evaluator. ");
        prompt.append("Analyze the following document and check if it meets the specified rules.\n\n");
        prompt.append("DOCUMENT CONTENT:\n");
        prompt.append(content);
        prompt.append("\n\nRULES TO CHECK:\n");
        
        for (int i = 0; i < rules.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, rules.get(i)));
        }
        
        prompt.append("\nYou must respond ONLY with valid JSON in this exact format:\n");
        prompt.append("{\n");
        prompt.append("  \"approval_status\": \"APPROVED\" or \"REJECTED\" or \"NEEDS_REVIEW\",\n");
        prompt.append("  \"reason\": \"Brief explanation of the decision\",\n");
        prompt.append("  \"confidence_score\": 0.0 to 1.0,\n");
        prompt.append("  \"rule_checks\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"rule_name\": \"Rule description\",\n");
        prompt.append("      \"passed\": true or false,\n");
        prompt.append("      \"details\": \"Specific findings\",\n");
        prompt.append("      \"confidence\": 0.0 to 1.0\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
   //Calls the LLM API with retry logic.
    private String callLlm(String prompt) {
        List<ChatMessage> messages = Arrays.asList(
            new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are a precise document compliance analyst. Always return valid JSON."),
            new ChatMessage(ChatMessageRole.USER.value(), prompt)
        );
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model(model)
            .messages(messages)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .build();
        
        try {
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            String response = result.getChoices().get(0).getMessage().getContent();
            log.debug("LLM response received: {} tokens", result.getUsage().getTotalTokens());
            return response;
            
        } catch (Exception e) {
            log.error("LLM API call failed", e);
            throw new RuntimeException("Failed to get LLM response", e);
        }
    }
    
//Parses the LLM's JSON response into structured result object.
    private EvaluationResult parseEvaluationResponse(String jsonResponse) {
        try {
            // Clean up response if it has markdown code blocks
            String cleaned = jsonResponse.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();
            
            // LOG THE RESPONSE
            log.info("=== PARSING LLM RESPONSE ===");
            log.info("Raw response: {}", jsonResponse);
            log.info("Cleaned response: {}", cleaned);
            
            JsonNode root = objectMapper.readTree(cleaned);
            
            // Parse rule checks - initialize as empty list to prevent null
            List<EvaluationResult.RuleCheckResult> ruleChecks = new ArrayList<>();
            JsonNode checksNode = root.get("rule_checks");
            
            log.info("rule_checks node: {}", checksNode);
            
            if (checksNode != null && checksNode.isArray()) {
                log.info("rule_checks is array with {} elements", checksNode.size());
                for (JsonNode checkNode : checksNode) {
                    ruleChecks.add(EvaluationResult.RuleCheckResult.builder()
                        .ruleName(checkNode.get("rule_name").asText())
                        .passed(checkNode.get("passed").asBoolean())
                        .details(checkNode.get("details").asText())
                        .confidence(checkNode.get("confidence").asDouble())
                        .build());
                }
            } else {
                log.warn("No rule_checks array found in response!");
            }
            
            log.info("Parsed {} rule checks", ruleChecks.size());
            
            return EvaluationResult.builder()
                .approvalStatus(EvaluationResult.ApprovalStatus.valueOf(
                    root.get("approval_status").asText()))
                .reason(root.get("reason").asText())
                .confidenceScore(root.get("confidence_score").asDouble())
                .ruleChecks(ruleChecks)
                .evaluatedAt(java.time.LocalDateTime.now())
                .build();
                
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response: {}", jsonResponse, e);
            throw new RuntimeException("Invalid LLM response format", e);
        } catch (Exception e) {
            log.error("Unexpected error parsing response: {}", jsonResponse, e);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }
}
