# 🚀 Advanced LLM Gateway & Token Optimizer

A sophisticated Spring Boot application that provides intelligent LLM token optimization, multi-provider routing, and stateful chat capabilities with document processing features.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [Configuration](#-configuration)
- [Docker Deployment](#-docker-deployment)
- [Development](#-development)
- [Testing](#-testing)
- [Contributing](#-contributing)
- [License](#-license)

## ✨ Features

### Core Capabilities
- **🔢 Token Counting**: Accurate token counting using OpenAI's tokenization algorithm (jtokkit)
- **📝 Document Optimization**: Intelligent summarization to fit within LLM context windows
- **💬 Stateful Chat**: Persistent conversation sessions with context management
- **📁 File Processing**: Upload and extract text from PDF, DOCX, and other document formats
- **🌐 Web Scraping**: Extract content from URLs for processing
- **🎯 Smart Routing**: Intelligent LLM provider selection based on request complexity

### Multi-Provider Support
- **⚡ Fast Tier**: Groq/Azure AI Inference for rapid responses
- **🧠 Gemini**: Google's Gemini AI for advanced reasoning
- **🔄 Fallback**: Automatic provider switching for reliability

### Advanced Features
- **📊 Token Optimization**: Real-time token usage optimization and compression
- **💾 Chat History**: Persistent chat sessions with intelligent compression
- **🎛️ Context Management**: Dynamic context window management
- **📈 Metrics & Analytics**: Detailed usage statistics and cost tracking
- **🔒 Production Ready**: Comprehensive error handling, logging, and validation
- **🐳 Docker Support**: Containerized deployment with health checks
- **📚 API Documentation**: Full Swagger/OpenAPI integration

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Web UI    │  │   cURL/CLI  │  │   Third-party Apps  │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                 Spring Boot Gateway                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                  Controllers                           │  │
│  │  ┌─────────────────┐  ┌─────────────────────────────┐  │  │
│  │  │   Token API     │  │     Advanced Chat API       │  │  │
│  │  │   (v1)          │  │        (v2)                │  │  │
│  │  └─────────────────┘  └─────────────────────────────┘  │  │
│  └─────────────────────────┬─────────────────────────────┘  │
│                            │                               │
│  ┌─────────────────────────▼─────────────────────────────┐  │
│  │                 Orchestration Layer                    │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │     AdvancedGatewayOrchestrationService        │  │  │
│  │  │  - Context Management                          │  │  │
│  │  │  - Chat History Compression                    │  │  │
│  │  │  - Smart Provider Routing                       │  │  │
│  │  │  - Token Optimization                           │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  └─────────────────────────┬─────────────────────────────┘  │
│                            │                               │
│  ┌─────────────────────────▼─────────────────────────────┐  │
│  │                   Service Layer                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌───────────────┐ │  │
│  │  │ Token       │  │ File Parser │  │ Web Scraper   │ │  │
│  │  │ Counter     │  │ Service     │  │ Service       │ │  │
│  │  └─────────────┘  └─────────────┘  └───────────────┘ │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌───────────────┐ │  │
│  │  │ LLM         │  │ Token       │  │ Chat History  │ │  │
│  │  │ Summarizer  │  │ Optimizer   │  │ Repository    │ │  │
│  │  └─────────────┘  └─────────────┘  └───────────────┘ │  │
│  └─────────────────────────┬─────────────────────────────┘  │
│                            │                               │
│  ┌─────────────────────────▼─────────────────────────────┐  │
│  │                 Provider Registry                     │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌───────────────┐ │  │
│  │  │ Fast Tier    │  │   Gemini     │  │ Provider      │ │  │
│  │  │ (Groq/Azure) │  │   Provider   │  │ Registry     │ │  │
│  │  └─────────────┘  └─────────────┘  └───────────────┘ │  │
│  └─────────────────────────┬─────────────────────────────┘  │
└────────────────────────────┼───────────────────────────────┘
                             │
         ┌───────────────────┴───────────────────┐
         │                                       │
         ▼                                       ▼
┌─────────────────────┐               ┌─────────────────────┐
│   External APIs    │               │   Local Processing  │
│  ┌─────────────┐   │               │  ┌─────────────┐   │
│  │ Groq/Azure  │   │               │  │   jtokkit   │   │
│  │   Inference │   │               │  │  (Tokenizer)│   │
│  └─────────────┘   │               │  └─────────────┘   │
│  ┌─────────────┐   │               │  ┌─────────────┐   │
│  │   Gemini    │   │               │  │  File       │   │
│  │     API     │   │               │  │  Parsers    │   │
│  └─────────────┘   │               │  └─────────────┘   │
└─────────────────────┘               └─────────────────────┘
```

## 📦 Prerequisites

- **Java 21** or higher
- **Maven 3.9+**
- **Docker** (optional, for containerized deployment)
- **API Keys**:
  - **Groq/Azure AI Inference Key** (free at [console.groq.com](https://console.groq.com/keys) or [Azure AI](https://azure.microsoft.com/en-us/products/ai-services/ai-inference))
  - **Gemini API Key** (optional, from [Google AI Studio](https://aistudio.google.com/app/apikey))

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/nikbarse1/demo-for-llm.git
cd demo-for-llm
```

### 2. Set Up Environment Variables

**Option A: Using application-local.properties (Recommended for Development)**

Create a file: `src/main/resources/application-local.properties`

```properties
# Fast Tier Configuration (Required)
llm.fast_tier.base_url=https://models.inference.ai.azure.com/chat/completions
llm.fast_tier.api.key=your-azure-groq-api-key-here
llm.fast_tier.model=gpt-4o-mini

# Gemini Configuration (Optional)
gemini.api.key=your-gemini-api-key-here
```

**Option B: Using Environment Variables**

```bash
# Fast Tier (Required)
export LLM_API_KEY=your-azure-groq-api-key-here
export LLM_MODEL=gpt-4o-mini

# Gemini (Optional)
export GEMINI_API_KEY=your-gemini-api-key-here
```

### 3. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Access the Application

- **Web UI**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

## 📚 API Documentation

The application provides two API versions:

### 📊 API v1 - Token Optimization
Basic token counting and document optimization endpoints.

#### Token Counter API

**Endpoint**: `POST /api/v1/tokens/count`

**Request**:
```json
{
  "text": "Hello, how are you today?"
}
```

**Response**:
```json
{
  "modelUsed": "gpt-4",
  "tokenCount": 7,
  "characterCount": 26,
  "estimatedCostNote": "1 token is roughly 4 characters in English.",
  "timestamp": "2026-06-26T12:00:00"
}
```

#### Document Optimization API

**Endpoint**: `POST /api/v1/optimize`

**Request**:
```json
{
  "document": "Your long document text here...",
  "contextWindow": 16000
}
```

**Response**:
```json
{
  "contextWindow": 16000,
  "originalTokens": 5000,
  "summaryTokens": 500,
  "reductionPercentage": 90.0,
  "headroomBefore": 11000,
  "headroomAfter": 15500,
  "summary": "Summarized content...",
  "timestamp": "2026-06-26T12:00:00"
}
```

### 💬 API v2 - Advanced Chat Gateway
Stateful chat with file upload, URL processing, and intelligent routing.

#### Chat API

**Endpoint**: `POST /api/v2/chat`

**Content-Type**: `multipart/form-data`

**Parameters**:
- `instruction` (required): The user's instruction or question
- `file` (optional): Document file (PDF, DOCX, TXT, etc.)
- `url` (optional): URL to scrape content from
- `chatId` (optional): Session identifier for conversation continuity
- `provider` (optional): LLM provider (`GEMINI`, `FAST_TIER`)
- `contextWindow` (optional): Context window size (default: 8192)
- `X-Developer-Mode` (header): Enable detailed metrics (default: false)

**cURL Example**:
```bash
# Basic chat
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=What is machine learning?"

# Chat with file upload
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=Summarize this document" \
  -F "file=@document.pdf"

# Chat with URL
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=What's the main topic of this page?" \
  -F "url=https://example.com/article"

# Continue conversation
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=Can you explain more about that?" \
  -F "chatId=abc-123-session-id"
```

**Response**:
```json
{
  "userReadableMessage": "Machine learning is a subset of AI...",
  "sourceType": "TEXT_ONLY",
  "wasOptimized": true,
  "optimizationMetrics": {
    "routingDecision": {
      "requestedProvider": "GEMINI",
      "executedProvider": "FAST_TIER",
      "actionTaken": "DOWNGRADED_TO_CHEAPER_MODEL"
    },
    "billingImpact": {
      "baselineTokens": 1500,
      "billedTokens": 800,
      "tokensSaved": 700,
      "savingsPercentage": 46.67
    },
    "compressionInternals": {
      "tokensProcessed": 1200,
      "tokensOutput": 600,
      "compressionReduction": 50.0
    }
  },
  "chatId": "abc-123-session-id"
}
```

## ⚙️ Configuration

### Application Properties

| Property | Description | Default |
|----------|-------------|---------|
| `llm.fast_tier.base_url` | Fast tier API base URL | Azure AI Inference |
| `llm.fast_tier.api.key` | Fast tier API key | Required |
| `llm.fast_tier.model` | Fast tier model | `gpt-4o-mini` |
| `gemini.api.key` | Gemini API key | Optional |
| `server.port` | Server port | `8080` |
| `spring.servlet.multipart.max-file-size` | Max file upload size | `50MB` |
| `web.scraper.timeout` | Web scraper timeout | `10000ms` |

### Supported Models

#### Fast Tier (Azure AI Inference/Groq)
- `gpt-4o-mini` (Fast, cost-effective)
- `llama-3.1-8b-instant` (Very fast)
- `llama-3.3-70b-versatile` (More accurate)
- `mixtral-8x7b-32768` (Large context window)

#### Gemini (Google AI)
- `gemini-1.5-flash` (Fast, versatile)
- `gemini-1.5-pro` (Advanced reasoning)
- `gemini-1.0-pro` (Legacy support)

### Smart Routing Logic

The system automatically routes requests based on:

1. **Request Complexity**: Simple queries use FAST_TIER
2. **Context Size**: Large documents may trigger compression
3. **Provider Availability**: Automatic fallback if provider fails
4. **Cost Optimization**: Prefer cheaper models for simple tasks

## 🐳 Docker Deployment

### Using Docker Compose (Recommended)

```bash
# Copy and configure environment file
cp .env.example .env
# Edit .env with your API keys

# Start the application
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

### Using Docker Directly

```bash
# Build the image
docker build -t llm-token-optimizer .

# Run the container with environment variables
docker run -d -p 8080:8080 \
  -e LLM_API_KEY=your-azure-groq-api-key-here \
  -e LLM_MODEL=gpt-4o-mini \
  -e GEMINI_API_KEY=your-gemini-api-key-here \
  --name llm-optimizer \
  llm-token-optimizer
```

## 💻 Development

### Project Structure

```
demo-for-llm/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── advancePlusOne/           # Advanced gateway features
│   │   │   │   ├── AdvancedGatewayOrchestrationService.java
│   │   │   │   ├── AiChatController2.java
│   │   │   │   ├── LlmProvider*.java     # Provider implementations
│   │   │   │   └── GatewayMessage.java   # Chat message model
│   │   │   ├── config/                   # Configuration classes
│   │   │   │   ├── LLMConfig.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── WebClientConfig.java
│   │   │   ├── dto/                      # Data transfer objects
│   │   │   │   └── UnifiedAnalysisResponse.java
│   │   │   ├── exception/                # Custom exceptions & handlers
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── LLMServiceException.java
│   │   │   ├── llmrouter/                # LLM routing services
│   │   │   │   ├── PrimaryLlmService.java
│   │   │   │   └── GeminiResponse.java
│   │   │   ├── *Controller.java          # REST controllers (v1 API)
│   │   │   ├── *Service.java             # Core business logic
│   │   │   └── *.java                    # Models and utilities
│   │   └── resources/
│   │       ├── static/                   # Frontend files
│   │       └── application.properties
│   └── test/                             # Unit tests
├── .github/workflows/                    # CI/CD pipelines
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TokenCounterServiceTest

# Run with coverage
mvn clean test jacoco:report
```

### Code Quality

```bash
# Check code style
mvn checkstyle:check

# Analyze dependencies
mvn dependency:analyze

# Security scan
mvn dependency-check:check
```

## 🧪 Testing

### Manual Testing with cURL

**Test Token Counter**:
```bash
curl -X POST http://localhost:8080/api/v1/tokens/count \
  -H "Content-Type: application/json" \
  -d '{
    "text": "The quick brown fox jumps over the lazy dog"
  }'
```

**Test Document Optimizer**:
```bash
curl -X POST http://localhost:8080/api/v1/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "document": "This is a very long document that needs to be summarized...",
    "contextWindow": 8000
  }'
```

### Using the Web UI

1. Navigate to http://localhost:8080
2. Use the **Token Counter** tab to count tokens
3. Use the **Document Optimizer** tab to summarize documents

## 🎯 Use Cases

### 📊 Token Optimization & Cost Management
- **Token Budget Management**: Calculate exact token counts before sending requests to manage costs
- **Context Window Optimization**: Automatically compress documents to fit within model limits
- **Cost Estimation**: Estimate API costs across different providers with detailed metrics
- **Smart Routing**: Automatically select the most cost-effective provider for each request

### 💬 Conversational AI Applications
- **Document Q&A**: Upload documents and ask questions about their content
- **Web Content Analysis**: Extract and analyze information from URLs
- **Multi-turn Conversations**: Maintain context across multiple interactions
- **Research Assistant**: Process multiple documents and provide insights

### 🏢 Enterprise Integration
- **RAG Pipeline Integration**: Pre-process documents for retrieval systems
- **Content Analysis**: Analyze large volumes of text efficiently
- **Customer Support**: Handle document-based customer queries
- **Knowledge Management**: Extract and summarize information from corporate documents

### 🔧 Development & Testing
- **Prompt Engineering**: Optimize prompts for maximum efficiency
- **API Gateway**: Single interface for multiple LLM providers
- **Load Testing**: Test different providers under various conditions
- **Performance Monitoring**: Track token usage and costs in real-time

## 🔧 Troubleshooting

### API Key Issues

**Problem**: `LLM service error` or `401 Unauthorized`

**Solution**: 
- Verify your Groq API key is correct
- Check environment variable is set: `echo $LLM_API_KEY`
- Ensure API key has proper permissions

### Port Already in Use

**Problem**: `Port 8080 is already in use`

**Solution**:
```bash
# Change port in application.properties
server.port=8081

# Or use environment variable
export SERVER_PORT=8081
```

### Docker Build Fails

**Problem**: Docker build fails with memory error

**Solution**:
```bash
# Increase Docker memory limit
docker build --memory=4g -t llm-token-optimizer .
```

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Development Guidelines

- Follow Java code conventions
- Add unit tests for new features
- Update documentation
- Ensure all tests pass
- Keep commits atomic and well-described

## 📖 Learning Resources

This project demonstrates:

- ✅ Spring Boot REST API development
- ✅ Integration with external LLM APIs
- ✅ Token counting for cost optimization
- ✅ Reactive programming with WebFlux
- ✅ Error handling and validation
- ✅ Docker containerization
- ✅ CI/CD with GitHub Actions
- ✅ API documentation with Swagger/OpenAPI

## 🔮 Future Enhancements

- [ ] Support for more LLM providers (OpenAI, Anthropic, etc.)
- [ ] Batch processing API
- [ ] Token cost calculator with pricing
- [ ] Multiple summarization strategies
- [ ] Streaming responses for large documents
- [ ] Rate limiting and caching
- [ ] User authentication
- [ ] Metrics and monitoring dashboard

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [jtokkit](https://github.com/knuddelsgmbh/jtokkit) - OpenAI tokenizer for Java
- [Groq](https://groq.com/) - Fast LLM inference API
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework

## 📧 Contact

For questions or support, please open an issue on GitHub.

---

**Made with ❤️ for the LLM community**
