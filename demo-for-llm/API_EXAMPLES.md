# API Examples

This document provides comprehensive examples for using the Advanced LLM Gateway & Token Optimizer API.

## 📚 Table of Contents

- [API v1 - Token Optimization](#api-v1---token-optimization)
  - [Token Counter API](#token-counter-api)
  - [Document Optimization API](#document-optimization-api)
- [API v2 - Advanced Chat Gateway](#api-v2---advanced-chat-gateway)
  - [Basic Chat](#basic-chat)
  - [File Upload](#file-upload)
  - [URL Processing](#url-processing)
  - [Stateful Conversations](#stateful-conversations)
  - [Provider Selection](#provider-selection)
- [Error Handling](#error-handling)
- [Code Examples](#code-examples)
- [Advanced Features](#advanced-features)

---

## API v1 - Token Optimization

### Token Counter API

**Endpoint**: `POST /api/v1/tokens/count`

### Basic Example

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/tokens/count \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Hello, how are you today?"
  }'
```

**Response:**
```json
{
  "modelUsed": "gpt-4",
  "tokenCount": 7,
  "characterCount": 26,
  "estimatedCostNote": "1 token is roughly 4 characters in English.",
  "timestamp": "2026-06-26T12:00:00"
}
```

### Long Text Example

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/tokens/count \
  -H "Content-Type: application/json" \
  -d '{
    "text": "The quick brown fox jumps over the lazy dog. This is a longer piece of text that demonstrates how the token counter works with multiple sentences and various punctuation marks!"
  }'
```

**Response:**
```json
{
  "modelUsed": "gpt-4",
  "tokenCount": 42,
  "characterCount": 178,
  "estimatedCostNote": "1 token is roughly 4 characters in English.",
  "timestamp": "2026-06-26T12:05:00"
}
```

### Code Example

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/tokens/count \
  -H "Content-Type: application/json" \
  -d '{
    "text": "function hello() {\n  console.log(\"Hello, World!\");\n}"
  }'
```

**Response:**
```json
{
  "modelUsed": "gpt-4",
  "tokenCount": 16,
  "characterCount": 51,
  "estimatedCostNote": "1 token is roughly 4 characters in English.",
  "timestamp": "2026-06-26T12:10:00"
}
```

---

## Document Optimization API

### Endpoint
```
POST /api/v1/optimize
```

### Basic Example

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "document": "This is a sample document that contains important information about machine learning and artificial intelligence. Machine learning is a subset of AI that focuses on training algorithms to learn from data. Deep learning, a subset of machine learning, uses neural networks with multiple layers.",
    "contextWindow": 8000
  }'
```

**Response:**
```json
{
  "contextWindow": 8000,
  "originalTokens": 52,
  "summaryTokens": 25,
  "reductionPercentage": 51.92,
  "headroomBefore": 7948,
  "headroomAfter": 7975,
  "summary": "• ML: subset of AI, trains algorithms from data\n• Deep learning: ML subset using multi-layer neural networks",
  "timestamp": "2026-06-26T12:15:00"
}
```

### Large Document Example

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "document": "Artificial Intelligence (AI) has revolutionized numerous industries over the past decade. From healthcare to finance, AI applications are transforming how we work and live. Machine learning, a core component of AI, enables systems to learn from data without explicit programming. Deep learning, utilizing neural networks with multiple layers, has achieved remarkable success in image recognition, natural language processing, and game playing. The future of AI holds immense potential, with ongoing research in areas like reinforcement learning, transfer learning, and explainable AI. However, ethical considerations around bias, privacy, and job displacement remain critical challenges that the AI community must address.",
    "contextWindow": 16000
  }'
```

**Response:**
```json
{
  "contextWindow": 16000,
  "originalTokens": 128,
  "summaryTokens": 45,
  "reductionPercentage": 64.84,
  "headroomBefore": 15872,
  "headroomAfter": 15955,
  "summary": "• AI revolutionized industries (healthcare, finance)\n• ML: learns from data w/o explicit programming\n• Deep learning: neural networks, excels in image recog, NLP, gaming\n• Future: reinforcement learning, transfer learning, explainable AI\n• Challenges: bias, privacy, job displacement",
  "timestamp": "2026-06-26T12:20:00"
}
```

### Custom Context Window

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "document": "Your very long document here...",
    "contextWindow": 4096
  }'
```

---

## Error Handling

### Invalid Request - Empty Text

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/tokens/count \
  -H "Content-Type: application/json" \
  -d '{
    "text": ""
  }'
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2026-06-26T12:25:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Text field is required and cannot be empty",
  "path": "/api/v1/tokens/count"
}
```

### Invalid Request - Missing Field

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "contextWindow": 8000
  }'
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2026-06-26T12:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Document field is required and cannot be empty",
  "path": "/api/v1/optimize"
}
```

### LLM Service Error

**Response (500 Internal Server Error):**
```json
{
  "timestamp": "2026-06-26T12:35:00",
  "status": 500,
  "error": "LLM Service Error",
  "message": "Failed to connect to LLM service",
  "path": "/api/v1/optimize"
}
```

---

## API v2 - Advanced Chat Gateway

### Basic Chat

**Endpoint**: `POST /api/v2/chat`

**Request**:
```bash
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=What is machine learning?" \
  -F "provider=FAST_TIER"
```

**Response**:
```json
{
  "userReadableMessage": "Machine learning is a subset of artificial intelligence...",
  "sourceType": "TEXT_ONLY",
  "wasOptimized": true,
  "optimizationMetrics": {
    "routingDecision": {
      "requestedProvider": "FAST_TIER",
      "executedProvider": "FAST_TIER",
      "actionTaken": "EXECUTED_AS_REQUESTED"
    },
    "billingImpact": {
      "baselineTokens": 25,
      "billedTokens": 25,
      "tokensSaved": 0,
      "savingsPercentage": 0.0
    }
  },
  "chatId": "generated-session-id-12345"
}
```

### File Upload

**Request**:
```bash
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=Summarize this document in 5 bullet points" \
  -F "file=@research-paper.pdf" \
  -F "provider=GEMINI" \
  -H "X-Developer-Mode: true"
```

**Response**:
```json
{
  "userReadableMessage": "• ML enables systems to learn from data without explicit programming\n• Deep learning uses neural networks with multiple layers\n• Applications include image recognition, NLP, and game playing\n• Future research focuses on explainable AI and transfer learning\n• Ethical considerations include bias and privacy concerns",
  "sourceType": "FILE",
  "wasOptimized": true,
  "optimizationMetrics": {
    "routingDecision": {
      "requestedProvider": "GEMINI",
      "executedProvider": "GEMINI",
      "actionTaken": "EXECUTED_AS_REQUESTED"
    },
    "billingImpact": {
      "baselineTokens": 2500,
      "billedTokens": 800,
      "tokensSaved": 1700,
      "savingsPercentage": 68.0
    },
    "compressionInternals": {
      "tokensProcessed": 2500,
      "tokensOutput": 150,
      "compressionReduction": 94.0
    }
  },
  "chatId": "generated-session-id-67890"
}
```

### URL Processing

**Request**:
```bash
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=What are the main topics discussed in this article?" \
  -F "url=https://example.com/tech-article" \
  -F "contextWindow=4096"
```

**Response**:
```json
{
  "userReadableMessage": "The article discusses artificial intelligence trends, cloud computing adoption, and cybersecurity challenges in 2024...",
  "sourceType": "URL",
  "wasOptimized": true,
  "chatId": "generated-session-id-11111"
}
```

### Stateful Conversations

**First Request**:
```bash
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=Explain quantum computing in simple terms" \
  -F "chatId=quantum-session-001"
```

**Follow-up Request**:
```bash
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=How does quantum entanglement relate to what you just explained?" \
  -F "chatId=quantum-session-001"
```

**Response (Follow-up)**:
```json
{
  "userReadableMessage": "Quantum entanglement is a phenomenon where quantum particles become interconnected...",
  "sourceType": "TEXT_ONLY",
  "wasOptimized": true,
  "optimizationMetrics": {
    "compressionInternals": {
      "compressionSummary": "Instruction Snapshot:\nExplain quantum computing...\n\nHistory Snapshot:\nQuantum computing uses qubits instead of classical bits..."
    }
  },
  "chatId": "quantum-session-001"
}
```

### Provider Selection

**Force Fast Tier**:
```bash
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=Simple question: What is 2+2?" \
  -F "provider=FAST_TIER"
```

**Force Gemini**:
```bash
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=Explain the implications of Godel's incompleteness theorems" \
  -F "provider=GEMINI"
```

**Smart Routing (Default)**:
```bash
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=What is the capital of France?"
```

---

## Code Examples

### JavaScript (Fetch API)

```javascript
// Token Counter (v1 API)
async function countTokens(text) {
  const response = await fetch('http://localhost:8080/api/v1/tokens/count', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  });
  return await response.json();
}

// Document Optimizer (v1 API)
async function optimizeDocument(document, contextWindow = 16000) {
  const response = await fetch('http://localhost:8080/api/v1/optimize', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ document, contextWindow }),
  });
  return await response.json();
}

// Advanced Chat (v2 API)
async function chatWithLLM(instruction, options = {}) {
  const formData = new FormData();
  formData.append('instruction', instruction);
  
  if (options.file) formData.append('file', options.file);
  if (options.url) formData.append('url', options.url);
  if (options.chatId) formData.append('chatId', options.chatId);
  if (options.provider) formData.append('provider', options.provider);
  if (options.contextWindow) formData.append('contextWindow', options.contextWindow);
  
  const response = await fetch('http://localhost:8080/api/v2/chat', {
    method: 'POST',
    body: formData,
    headers: { 'X-Developer-Mode': options.devMode || 'false' }
  });
  return await response.json();
}

// Usage examples
async function demonstrateAPIs() {
  // Token counting
  const tokens = await countTokens('Hello, world!');
  console.log('Token count:', tokens.tokenCount);
  
  // Document optimization
  const optimized = await optimizeDocument('Long document text...', 8000);
  console.log('Reduction:', optimized.reductionPercentage + '%');
  
  // Basic chat
  const chat1 = await chatWithLLM('What is AI?');
  console.log('Response:', chat1.userReadableMessage);
  
  // Chat with file
  const fileInput = document.querySelector('input[type="file"]');
  const chat2 = await chatWithLLM('Summarize this document', { 
    file: fileInput.files[0],
    provider: 'GEMINI'
  });
  console.log('File analysis:', chat2.userReadableMessage);
  
  // Continue conversation
  const chat3 = await chatWithLLM('Tell me more', { 
    chatId: chat2.chatId 
  });
  console.log('Continuation:', chat3.userReadableMessage);
}
```

### Python (Requests)

```python
import requests

# Token Counter
def count_tokens(text):
    url = 'http://localhost:8080/api/v1/tokens/count'
    payload = {'text': text}
    
    response = requests.post(url, json=payload)
    response.raise_for_status()
    
    return response.json()

# Usage
result = count_tokens('Hello, world!')
print(f"Token count: {result['tokenCount']}")

# Document Optimizer
def optimize_document(document, context_window=16000):
    url = 'http://localhost:8080/api/v1/optimize'
    payload = {
        'document': document,
        'contextWindow': context_window
    }
    
    response = requests.post(url, json=payload)
    response.raise_for_status()
    
    return response.json()

# Usage
result = optimize_document('Long document text...', 8000)
print(f"Reduction: {result['reductionPercentage']}%")
print(f"Summary: {result['summary']}")
```

### Java (RestTemplate)

```java
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class LLMClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl = "http://localhost:8080/api/v1";
    
    public TokenResponse countTokens(String text) {
        String url = baseUrl + "/tokens/count";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        TokenRequest request = new TokenRequest();
        request.setText(text);
        
        HttpEntity<TokenRequest> entity = new HttpEntity<>(request, headers);
        
        return restTemplate.postForObject(url, entity, TokenResponse.class);
    }
    
    public OptimizationResponse optimizeDocument(String document, int contextWindow) {
        String url = baseUrl + "/optimize";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        OptimizationRequest request = new OptimizationRequest();
        request.setDocument(document);
        request.setContextWindow(contextWindow);
        
        HttpEntity<OptimizationRequest> entity = new HttpEntity<>(request, headers);
        
        return restTemplate.postForObject(url, entity, OptimizationResponse.class);
    }
}

// Usage
LLMClient client = new LLMClient();
TokenResponse tokenResult = client.countTokens("Hello, world!");
System.out.println("Token count: " + tokenResult.getTokenCount());

OptimizationResponse optResult = client.optimizeDocument("Long text...", 8000);
System.out.println("Reduction: " + optResult.getReductionPercentage() + "%");
```

### cURL with Variables

```bash
#!/bin/bash

# Set variables
API_URL="http://localhost:8080/api/v1"
TEXT="Hello, how are you today?"
DOCUMENT="This is a long document that needs to be optimized..."
CONTEXT_WINDOW=16000

# Count tokens
echo "Counting tokens..."
curl -X POST "$API_URL/tokens/count" \
  -H "Content-Type: application/json" \
  -d "{\"text\":\"$TEXT\"}" \
  | jq '.'

# Optimize document
echo "Optimizing document..."
curl -X POST "$API_URL/optimize" \
  -H "Content-Type: application/json" \
  -d "{\"document\":\"$DOCUMENT\",\"contextWindow\":$CONTEXT_WINDOW}" \
  | jq '.'
```

---

## Advanced Features

### Developer Mode

Enable detailed metrics by setting the `X-Developer-Mode: true` header:

```bash
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=Explain blockchain" \
  -H "X-Developer-Mode: true"
```

This returns comprehensive optimization metrics including:
- Token compression statistics
- Provider routing decisions
- Cost optimization details
- Context window utilization

### Context Window Management

Adjust context window size based on your needs:

```bash
# Small context for simple queries
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=What is 2+2?" \
  -F "contextWindow=1024"

# Large context for complex documents
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=Analyze this research paper" \
  -F "file=@paper.pdf" \
  -F "contextWindow=32768"
```

### Smart Routing Examples

The system automatically routes requests based on complexity:

```bash
# Simple query - routes to FAST_TIER
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=What is the weather?"

# Complex query - may route to GEMINI
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=Explain the philosophical implications of quantum mechanics"

# With document - routes based on content
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=Summarize this legal document" \
  -F "file=@contract.pdf"
```

---

## Testing with Postman

### Import Collection

You can import these examples into Postman:

1. Create a new collection named "Advanced LLM Gateway"
2. Add requests with the examples above
3. Set base URL as a variable: `{{baseUrl}}` = `http://localhost:8080`

### Environment Variables

```json
{
  "baseUrl": "http://localhost:8080",
  "apiVersion": "v2",
  "defaultProvider": "FAST_TIER",
  "defaultContextWindow": "8192"
}
```

### Postman Request Examples

**Token Count (v1)**:
```json
{
  "method": "POST",
  "header": [
    {
      "key": "Content-Type",
      "value": "application/json"
    }
  ],
  "body": {
    "mode": "raw",
    "raw": "{\"text\":\"Sample text for token counting\"}"
  },
  "url": {
    "raw": "{{baseUrl}}/api/v1/tokens/count",
    "host": ["{{baseUrl}}"],
    "path": ["api", "v1", "tokens", "count"]
  }
}
```

**Advanced Chat (v2)**:
```json
{
  "method": "POST",
  "header": [
    {
      "key": "X-Developer-Mode",
      "value": "true"
    }
  ],
  "body": {
    "mode": "formdata",
    "formdata": [
      {
        "key": "instruction",
        "value": "Explain artificial intelligence"
      },
      {
        "key": "provider",
        "value": "GEMINI"
      }
    ]
  },
  "url": {
    "raw": "{{baseUrl}}/api/v2/chat",
    "host": ["{{baseUrl}}"],
    "path": ["api", "v2", "chat"]
  }
}
```

---

## Rate Limiting Considerations

Currently, the API does not implement rate limiting, but when deploying to production, consider:

- Implementing rate limiting per IP or API key
- Setting appropriate timeout values for file uploads
- Caching frequently requested token counts
- Monitoring API usage and costs
- Implementing quotas for different user tiers

---

## Best Practices

### API Usage
1. **Choose the Right API Version**: Use v1 for simple token operations, v2 for advanced features
2. **Provider Selection**: Let smart routing handle simple queries, specify providers for complex tasks
3. **Context Management**: Adjust context windows based on document complexity
4. **Error Handling**: Always implement proper error handling for network issues
5. **File Optimization**: Compress files before upload for faster processing

### Performance Optimization
1. **Batch Processing**: For multiple documents, consider parallel processing
2. **Timeouts**: Set appropriate timeout values for large documents
3. **Caching**: Cache responses for repeated queries
4. **Validation**: Validate input before sending to API
5. **Monitoring**: Track token usage and costs in real-time

### Security
1. **API Key Management**: Rotate API keys regularly
2. **Input Validation**: Validate all user inputs
3. **File Security**: Scan uploaded files for malware
4. **Access Control**: Implement proper authentication in production
5. **Audit Logging**: Log all API calls for security monitoring

---

For more information, see:
- [README.md](README.md) - Full documentation
- [Swagger UI](http://localhost:8080/swagger-ui.html) - Interactive API documentation
- [SETUP.md](SETUP.md) - Setup instructions
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines
