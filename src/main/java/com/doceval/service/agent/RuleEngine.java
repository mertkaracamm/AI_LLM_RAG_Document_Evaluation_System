package com.doceval.service.agent;

import com.doceval.service.model.EvaluationRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

// Rule management
@Slf4j
@Component
public class RuleEngine {
    
    private final Map<String, EvaluationRule> rules = new HashMap<>();
    
    @PostConstruct
    public void initializeDefaultRules() {
        log.info("Initializing default evaluation rules");
        
        // Rule 1: Signature requirement
        addRule(EvaluationRule.builder()
            .id("rule-001")
            .name("Signature Verification")
            .description("Document must contain a signature block or digital signature confirmation")
            .type(EvaluationRule.RuleType.SIGNATURE_CHECK)
            .priority(1)
            .weight(0.3)
            .mandatory(true)
            .build());
        
        // Rule 2: Date validation
        addRule(EvaluationRule.builder()
            .id("rule-002")
            .name("Date Validation")
            .description("Document date must be present and within acceptable range (not future-dated)")
            .type(EvaluationRule.RuleType.DATE_VALIDATION)
            .priority(1)
            .weight(0.2)
            .mandatory(true)
            .build());
        
        // Rule 3: Approval clause
        addRule(EvaluationRule.builder()
            .id("rule-003")
            .name("Approval Clause")
            .description("Must contain explicit approval language such as 'approved by', 'authorized by', or 'confirmed by both parties'")
            .type(EvaluationRule.RuleType.KEYWORD_PRESENCE)
            .priority(2)
            .weight(0.25)
            .mandatory(true)
            .build());
        
        // Rule 4: Party identification
        addRule(EvaluationRule.builder()
            .id("rule-004")
            .name("Party Identification")
            .description("Document must clearly identify all parties involved (names, company names, or official titles)")
            .type(EvaluationRule.RuleType.SEMANTIC_MATCH)
            .priority(2)
            .weight(0.15)
            .mandatory(false)
            .build());
        
        // Rule 5: Completeness check
        addRule(EvaluationRule.builder()
            .id("rule-005")
            .name("Document Completeness")
            .description("Document should not contain placeholders like [TO BE FILLED], [TBD], or blank fields in critical sections")
            .type(EvaluationRule.RuleType.CUSTOM_LLM)
            .priority(3)
            .weight(0.1)
            .mandatory(false)
            .build());
        
        log.info("Initialized {} evaluation rules", rules.size());
    }
    
    //Adds a new rule to the engine.
    public void addRule(EvaluationRule rule) {
        rules.put(rule.getId(), rule);
        log.debug("Added rule: {} - {}", rule.getId(), rule.getName());
    }
    
   
    public List<EvaluationRule> getAllRules() {
        return new ArrayList<>(rules.values());
    }
    
    
    public List<EvaluationRule> getRulesByType(EvaluationRule.RuleType type) {
        return rules.values().stream()
            .filter(r -> r.getType() == type)
            .toList();
    }
    
    
    public List<EvaluationRule> getMandatoryRules() {
        return rules.values().stream()
            .filter(EvaluationRule::getMandatory)
            .toList();
    }
    
    
    public Optional<EvaluationRule> getRule(String id) {
        return Optional.ofNullable(rules.get(id));
    }
    
    
    public void removeRule(String id) {
        rules.remove(id);
        log.info("Removed rule: {}", id);
    }
}
