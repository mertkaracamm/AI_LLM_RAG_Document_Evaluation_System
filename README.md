# LLM Document Evaluation Service

An intelligent document compliance evaluation system that leverages Large Language Models (LLMs) and Retrieval-Augmented Generation (RAG) to automate document approval workflows.

## 🎯 Overview

This service addresses the challenge of manual document review processes in financial and legal domains. Traditional workflows require external reviewers to manually validate 10-20 documents per batch, checking for compliance with rules such as signature presence, date validation, and specific wording requirements. This process is slow, error-prone, and inconsistent.

Our solution uses an **agentic AI system** powered by GPT-4 that:
- Parses PDF documents and extracts content
- Evaluates documents against configurable compliance rules
- Uses semantic search (RAG) to find relevant context from historical approvals
- Provides structured, auditable decisions with confidence scores
- Maintains complete decision trails for regulatory compliance

## 🏗️ Architecture

```
┌─────────────────┐
│   PDF Upload    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────────┐
│  PDF Processing │─────▶│  Text Extraction │
└─────────────────┘      └────────┬─────────┘
                                  │
                                  ▼
                         ┌────────────────┐
                         │   Embedding    │
                         │   Generation   │
                         └────────┬───────┘
                                  │
         ┌────────────────────────┼────────────────────────┐
         │                        │                        │
         ▼                        ▼                        ▼
┌─────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ Vector Storage  │    │  Agent Workflow  │    │  Rule Engine     │
│    (Redis)      │◀───│  Orchestrator    │───▶│  Evaluation      │
└─────────────────┘    └──────────────────┘    └──────────────────┘
                                  │
                                  ▼
                         ┌────────────────┐
                         │  LLM Service   │
                         │   (GPT-4)      │
                         └────────┬───────┘
                                  │
                                  ▼
                         ┌────────────────┐
                         │ Evaluation     │
                         │ Result         │
                         └────────────────┘
```

## ✨ Key Features

### 🤖 Agentic Workflow System
- Multi-step planning and execution
- Context-aware decision making
- State management across evaluation steps
- Complete audit trails

### 📊 RAG Implementation
- Semantic search using vector embeddings
- Redis Vector Similarity Search
- Historical document context retrieval
- Confidence adjustment based on similar cases

### 🔍 LLM Integration
- GPT-4 powered evaluation
- Structured JSON outputs
- Custom prompt engineering
- Error handling and retry logic

### 📝 Rule Engine
- Configurable compliance rules
- Multiple rule types (keyword, semantic, custom)
- Priority and weight-based scoring
- Mandatory vs. optional rules

### 🎯 Production-Ready
- RESTful API design
- Comprehensive error handling
- Logging and monitoring
- API documentation (Swagger/OpenAPI)

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Redis (with RedisJSON and RediSearch modules)
- OpenAI API key
- Maven 3.6+

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/mertkaracamm/RedisDocLLMEvaluationService.git
cd RedisDocLLMEvaluationService
```

2. **Install Redis Stack** (includes vector search)
```bash
# On macOS
brew install redis-stack

# On Linux
curl -fsSL https://packages.redis.io/gpg | sudo gpg --dearmor -o /usr/share/keyrings/redis-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/redis-archive-keyring.gpg] https://packages.redis.io/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/redis.list
sudo apt-get update
sudo apt-get install redis-stack-server

# Start Redis
redis-stack-server
```

3. **Configure environment variables**
```bash
# Create .env file or set in application.properties
export OPENAI_API_KEY=your-api-key-here
```

4. **Build and run**
```bash
./mvnw clean install
./mvnw spring-boot:run
```

The service will start on `http://localhost:8080`

## 📖 API Documentation

Once running, access the interactive API documentation at:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

### Quick API Examples

**1. Upload a document**
```bash
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -F "file=@/path/to/document.pdf"
```

Response:
```json
{
  "status": "success",
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "contract.pdf",
  "wordCount": 1250,
  "pageCount": 5
}
```

**2. Evaluate the document**
```bash
curl -X POST http://localhost:8080/api/v1/documents/{documentId}/evaluate
```

Response:
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "decision": "APPROVED",
  "reasoning": "All mandatory compliance rules passed. Document contains required signatures, valid dates, and approval language from both parties.",
  "confidence": 0.92,
  "timestamp": "2024-01-15T10:30:00",
  "ruleResults": [
    {
      "rule": "Signature Verification",
      "passed": true,
      "explanation": "Found signature block for John Doe, Authorized Representative"
    },
    {
      "rule": "Date Validation",
      "passed": true,
      "explanation": "Document dated January 15, 2024 - within acceptable range"
    },
    {
      "rule": "Approval Clause",
      "passed": true,
      "explanation": "Contains explicit approval language: 'approved by both parties'"
    }
  ]
}
```

**3. Semantic search across documents**
```bash
curl "http://localhost:8080/api/v1/documents/search?query=financial+approval&limit=5"
```

**4. Combined upload and evaluate**
```bash
curl -X POST http://localhost:8080/api/v1/documents/upload-and-evaluate \
  -F "file=@/path/to/document.pdf"
```

## 🔧 Configuration

Key configuration options in `application.properties`:

```properties
# OpenAI Configuration
openai.api.key=${OPENAI_API_KEY}
openai.model=gpt-4-turbo-preview
openai.temperature=0.3
openai.max-tokens=2000

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Evaluation Settings
evaluation.rules.max-retries=3
evaluation.cache.ttl=3600
evaluation.embedding.dimension=1536
```

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/com/sequra/llmevaluation/
│   │   ├── agent/                      # Agentic workflow components
│   │   │   ├── EvaluationAgent.java   # Main agent orchestrator
│   │   │   └── RuleEngine.java        # Compliance rule management
│   │   ├── config/                     # Configuration classes
│   │   ├── controller/                 # REST API endpoints
│   │   ├── dto/                        # Data transfer objects
│   │   ├── exception/                  # Exception handling
│   │   ├── model/                      # Domain models
│   │   └── service/                    # Business logic
│   │       ├── DocumentService.java    # Main document operations
│   │       ├── LlmService.java        # OpenAI integration
│   │       ├── PdfProcessingService.java
│   │       └── VectorSearchService.java # RAG implementation
│   └── resources/
│       └── application.properties
└── test/
    └── java/                           # Unit and integration tests
```

## 🧪 Testing

Run the test suite:
```bash
./mvnw test
```

Note: Integration tests require a valid OpenAI API key. For CI/CD pipelines, consider mocking the LLM service.

## 🎓 Technical Highlights

### Agent-Based Architecture
The system implements a multi-step agentic workflow:
1. **Planning**: Determines applicable rules based on document type
2. **Context Retrieval**: Finds similar documents using RAG
3. **Execution**: Runs LLM evaluation with retrieved context
4. **Validation**: Adjusts confidence based on historical data
5. **Audit**: Logs complete decision trail

### RAG Implementation
Uses Redis Vector Similarity Search for semantic document retrieval:
- Generates embeddings using OpenAI's `text-embedding-3-small`
- Stores in Redis with document metadata
- Performs cosine similarity search
- Retrieves top-K similar documents for context

### Prompt Engineering
Structured prompts guide the LLM to return consistent JSON:
- Clear instruction formatting
- JSON schema enforcement
- Example-based guidance
- Error-resistant parsing

## 🔜 Future Enhancements

- [ ] Support for multiple LLM providers (Anthropic Claude, Google Gemini)
- [ ] Advanced NER for entity extraction
- [ ] Document classification before evaluation
- [ ] Batch processing for high-volume workflows
- [ ] Web UI for document management
- [ ] Real-time evaluation status tracking
- [ ] Custom rule creation via API
- [ ] Integration with document management systems

## 📊 Performance

- **Document Processing**: ~2-3 seconds for typical 5-page PDF
- **LLM Evaluation**: ~3-5 seconds per document
- **Semantic Search**: ~100ms for 1000 indexed documents
- **Total End-to-End**: ~6-10 seconds

## 🙏 Acknowledgments

- Built with Spring Boot, OpenAI GPT-4, and Redis Stack
- Inspired by modern RAG architectures and agentic AI systems
- Designed for real-world compliance automation needs

---

**Note**: This is a demonstration project showcasing LLM integration, RAG implementation, and agentic workflow orchestration. For production use, additional security, scalability, and compliance measures should be implemented.
