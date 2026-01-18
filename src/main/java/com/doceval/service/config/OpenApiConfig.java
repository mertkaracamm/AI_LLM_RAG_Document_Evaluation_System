package com.doceval.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("LLM Document Evaluation Service API")
                .version("1.0.0")
                .description("""
                    Intelligent document compliance evaluation system using Large Language Models
                    and Retrieval-Augmented Generation (RAG).
                    
                    Key Features:
                    - PDF document processing and text extraction
                    - Semantic search using vector embeddings
                    - LLM-based rule evaluation with GPT-4
                    - Agentic workflow orchestration
                    - Comprehensive audit trails
                    
                    This API enables automated compliance checking for financial and legal documents,
                    replacing manual review processes with AI-powered validation.
                    """)
                .contact(new Contact()
                    .name("Mert Karacam")
                    .url("https://github.com/mertkaracamm")
                    )
            );
    }
}
