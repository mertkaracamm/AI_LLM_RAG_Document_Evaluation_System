package com.doceval.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {
    
    @NotBlank(message = "Filename is required")
    private String filename;
    
    @NotBlank(message = "Content type is required")
    private String contentType;
    
    @NotNull(message = "File content is required")
    private byte[] fileContent;
    
    // Optional: specify document type for targeted rule evaluation
    private String documentType;
}
