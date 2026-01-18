package com.doceval.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

// Main application
@SpringBootApplication
//@EnableCaching
public class LlmEvaluationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmEvaluationServiceApplication.class, args);
    }
}
