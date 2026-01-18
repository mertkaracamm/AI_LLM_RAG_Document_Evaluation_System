# API Usage Guide

## Quick Start Tutorial

This guide walks you through a complete document evaluation workflow.

## Step 1: Start the Services

```bash
# Terminal 1: Start Redis
redis-stack-server

# Terminal 2: Start the application
export OPENAI_API_KEY=your-key-here
./mvnw spring-boot:run
```

Wait for the application to start. You should see:
```
Started LlmEvaluationServiceApplication in X.XXX seconds
```

## Step 2: Upload a Document

Create a sample PDF or use an existing one, then upload it:

```bash
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -F "file=@sample-contract.pdf" \
  -H "Accept: application/json"
```

**Expected Response:**
```json
{
  "status": "success",
  "message": "Document uploaded successfully",
  "documentId": "abc123-def456-ghi789",
  "filename": "sample-contract.pdf",
  "wordCount": 1500,
  "pageCount": 3
}
```

Save the `documentId` for the next steps.

## Step 3: Trigger Evaluation

```bash
# Replace {documentId} with the ID from Step 2
curl -X POST http://localhost:8080/api/v1/documents/{documentId}/evaluate \
  -H "Accept: application/json"
```

**Expected Response:**
```json
{
  "documentId": "abc123-def456-ghi789",
  "status": "COMPLETED",
  "decision": "APPROVED",
  "reasoning": "Document meets all mandatory compliance requirements...",
  "confidence": 0.87,
  "timestamp": "2024-01-15T14:30:00",
  "ruleResults": [
    {
      "rule": "Signature Verification",
      "passed": true,
      "explanation": "Digital signature found for both parties"
    },
    {
      "rule": "Date Validation",
      "passed": true,
      "explanation": "Document date is January 10, 2024 - valid"
    },
    {
      "rule": "Approval Clause",
      "passed": true,
      "explanation": "Contains 'approved by authorized representative'"
    },
    {
      "rule": "Party Identification",
      "passed": true,
      "explanation": "Both parties clearly identified with full names"
    },
    {
      "rule": "Document Completeness",
      "passed": true,
      "explanation": "No placeholder text found"
    }
  ]
}
```

## Step 4: Retrieve Results Later

```bash
curl http://localhost:8080/api/v1/documents/{documentId}/result
```

## Step 5: Semantic Search

Search for documents containing specific content:

```bash
curl "http://localhost:8080/api/v1/documents/search?query=payment+terms&limit=5"
```

**Response:**
```json
[
  {
    "documentId": "abc123-def456-ghi789",
    "filename": "sample-contract.pdf",
    "similarity": 0.94,
    "excerpt": "Payment terms: Net 30 days from invoice date..."
  },
  {
    "documentId": "xyz789-uvw123-rst456",
    "filename": "vendor-agreement.pdf",
    "similarity": 0.88,
    "excerpt": "The payment schedule is as follows..."
  }
]
```

## Advanced: Upload and Evaluate in One Call

```bash
curl -X POST http://localhost:8080/api/v1/documents/upload-and-evaluate \
  -F "file=@document.pdf" \
  -H "Accept: application/json"
```

This combines upload and evaluation into a single request.

## Understanding the Results

### Decision Types
- **APPROVED**: All mandatory rules passed
- **REJECTED**: One or more mandatory rules failed
- **NEEDS_REVIEW**: Uncertain result (confidence < 0.7)

### Confidence Scores
- `0.9 - 1.0`: Very high confidence
- `0.7 - 0.9`: Good confidence
- `0.5 - 0.7`: Moderate confidence (manual review recommended)
- `< 0.5`: Low confidence (manual review required)

### Rule Types
1. **SIGNATURE_CHECK**: Verifies signature presence
2. **DATE_VALIDATION**: Validates document dates
3. **KEYWORD_PRESENCE**: Checks for specific terms
4. **SEMANTIC_MATCH**: Uses semantic similarity
5. **CUSTOM_LLM**: Custom LLM-based validation

## Common Use Cases

### Batch Processing
```bash
for file in *.pdf; do
  echo "Processing: $file"
  curl -X POST http://localhost:8080/api/v1/documents/upload-and-evaluate \
    -F "file=@$file" \
    -o "result-${file%.pdf}.json"
  sleep 2  # Rate limiting
done
```

### Integration Example (Python)
```python
import requests

# Upload
with open('document.pdf', 'rb') as f:
    response = requests.post(
        'http://localhost:8080/api/v1/documents/upload',
        files={'file': f}
    )
doc_id = response.json()['documentId']

# Evaluate
eval_response = requests.post(
    f'http://localhost:8080/api/v1/documents/{doc_id}/evaluate'
)
result = eval_response.json()

print(f"Decision: {result['decision']}")
print(f"Confidence: {result['confidence']}")
```

## Troubleshooting

### "Document not found"
- Ensure you're using the correct document ID
- Check that the document was successfully uploaded

### "LLM API call failed"
- Verify your OpenAI API key is valid
- Check your API usage limits
- Ensure internet connectivity

### "PDF text extraction failed"
- PDF might be encrypted or corrupted
- Try with a different PDF

### Slow responses
- First request is slower due to model initialization
- Subsequent requests use cached connections
- Consider scaling Redis for production

## API Reference

Full API documentation is available at:
- http://localhost:8080/swagger-ui.html

## Rate Limits

The service doesn't impose rate limits by default, but OpenAI API has limits:
- Free tier: ~3 requests/minute
- Paid tier: Higher limits based on plan

Consider implementing request queuing for production use.

## Security Notes

For production deployment:
1. Enable HTTPS
2. Add API authentication (JWT, OAuth2)
3. Implement request validation
4. Add input sanitization
5. Set up proper CORS policies
6. Use environment variables for secrets
7. Enable Redis authentication

## Next Steps

1. Try uploading different document types
2. Experiment with semantic search
3. Review the evaluation results in detail
4. Integrate with your existing systems
5. Customize rules in `RuleEngine.java`

For more information, see the main [README.md](README.md)
