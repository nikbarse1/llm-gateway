# Setup Guide

This comprehensive guide will help you set up the Advanced LLM Gateway & Token Optimizer application on your local machine.

## 📋 Prerequisites Checklist

Before you begin, ensure you have:

- [ ] **Java 21** or higher installed
- [ ] **Maven 3.9+** installed
- [ ] **Git** installed
- [ ] **API Keys**:
  - [ ] Azure AI Inference or Groq API key (required)
  - [ ] Gemini API key (optional, for advanced features)
- [ ] (Optional) **Docker** and **Docker Compose** for containerized deployment
- [ ] (Optional) **IDE** - IntelliJ IDEA recommended for Java development

## 🔍 Verify Prerequisites

### Check Java Version
```bash
java -version
# Should show: java version "21.x.x" or higher
```

### Check Maven Version
```bash
mvn -version
# Should show: Apache Maven 3.9.x or higher
```

### Check Git
```bash
git --version
# Should show: git version 2.x.x or higher
```

### Check Docker (Optional)
```bash
docker --version
docker-compose --version
# Should show Docker and Docker Compose versions
```

## 🚀 Installation Steps

### Step 1: Clone the Repository

```bash
git clone https://github.com/nikbarse1/demo-for-llm.git
cd demo-for-llm
```

### Step 2: Get Your API Keys

#### Fast Tier API (Required)

**Option 1: Azure AI Inference (Recommended)**
1. Visit [Azure AI Inference](https://azure.microsoft.com/en-us/products/ai-services/ai-inference)
2. Sign up for Azure account (free tier available)
3. Create an AI Inference resource
4. Get your API key and endpoint

**Option 2: Groq API**
1. Visit [https://console.groq.com/keys](https://console.groq.com/keys)
2. Sign up or log in
3. Create a new API key
4. Copy the key (starts with `gsk_`)

#### Gemini API (Optional)

1. Visit [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with Google account
3. Create a new API key
4. Copy the key (starts with `AIza`)

### Step 3: Configure Environment Variables

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

**Windows (PowerShell):**
```powershell
# Fast Tier (Required)
$env:LLM_API_KEY="your-azure-groq-api-key-here"
$env:LLM_MODEL="gpt-4o-mini"

# Gemini (Optional)
$env:GEMINI_API_KEY="your-gemini-api-key-here"
```

**Windows (Command Prompt):**
```cmd
# Fast Tier (Required)
set LLM_API_KEY=your-azure-groq-api-key-here
set LLM_MODEL=gpt-4o-mini

# Gemini (Optional)
set GEMINI_API_KEY=your-gemini-api-key-here
```

**macOS/Linux:**
```bash
# Fast Tier (Required)
export LLM_API_KEY="your-azure-groq-api-key-here"
export LLM_MODEL="gpt-4o-mini"

# Gemini (Optional)
export GEMINI_API_KEY="your-gemini-api-key-here"
```

**Option C: Using .env file (for Docker)**

Copy the example file:
```bash
cp .env.example .env
```

Edit `.env` with your API keys:
```bash
# Fast Tier Configuration (Required)
LLM_API_KEY=your-azure-groq-api-key-here
LLM_MODEL=gpt-4o-mini

# Gemini Configuration (Optional)
GEMINI_API_KEY=your-gemini-api-key-here
```

### Step 4: Build the Project

```bash
mvn clean install
```

This will:
- Download all dependencies
- Compile the code
- Run tests
- Create the JAR file

### Step 5: Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR directly:
```bash
java -jar target/demo-for-llm-0.0.1-SNAPSHOT.jar
```

### Step 6: Verify Installation

Open your browser and navigate to:

- **Web UI**: [http://localhost:8080](http://localhost:8080)
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

You should see the application running!

## 🐳 Docker Setup (Alternative)

If you prefer using Docker:

### Step 1: Build Docker Image

```bash
docker build -t llm-token-optimizer .
```

### Step 2: Run with Docker Compose

```bash
# Make sure .env file has your API key
docker-compose up -d
```

### Step 3: View Logs

```bash
docker-compose logs -f
```

### Step 4: Stop the Application

```bash
docker-compose down
```

## 🧪 Testing the Setup

### Test 1: Token Counter API (v1)

```bash
curl -X POST http://localhost:8080/api/v1/tokens/count \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello, how are you today?"}'
```

Expected response:
```json
{
  "modelUsed": "gpt-4",
  "tokenCount": 7,
  "characterCount": 26,
  "estimatedCostNote": "1 token is roughly 4 characters in English.",
  "timestamp": "2026-06-26T12:00:00"
}
```

### Test 2: Document Optimizer API (v1)

```bash
curl -X POST http://localhost:8080/api/v1/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "document": "This is a test document that will be summarized by the LLM service.",
    "contextWindow": 16000
  }'
```

### Test 3: Advanced Chat API (v2)

```bash
# Basic chat
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=What is machine learning?"

# Chat with provider selection
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=Explain quantum computing" \
  -F "provider=GEMINI" \
  -H "X-Developer-Mode: true"

# Chat with file upload (create a test file first)
echo "This is a test document about artificial intelligence." > test.txt
curl -X POST http://localhost:8080/api/v2/chat \
  -F "instruction=Summarize this document" \
  -F "file=@test.txt"
```

### Test 4: Web UI

1. Open [http://localhost:8080](http://localhost:8080)
2. Try the Token Counter tab (v1 API)
3. Try the Document Optimizer tab (v1 API)
4. Try the Advanced Chat features (v2 API)

### Test 5: Health and Metrics

```bash
# Health check
curl http://localhost:8080/actuator/health

# Application info
curl http://localhost:8080/actuator/info

# Metrics (if enabled)
curl http://localhost:8080/actuator/metrics
```

## 🔧 Troubleshooting

### Issue: Port 8080 Already in Use

**Solution 1**: Change the port
```properties
# In application.properties or application-local.properties
server.port=8081
```

**Solution 2**: Kill the process using port 8080
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# macOS/Linux
lsof -ti:8080 | xargs kill -9
```

### Issue: API Key Not Working

**Symptoms**: 401 Unauthorized or LLM service errors

**Solutions**:
1. Verify your API key is correct
2. Check if the key has expired
3. Ensure no extra spaces in the key
4. Try regenerating the key at [console.groq.com](https://console.groq.com/keys)

### Issue: Maven Build Fails

**Solution**: Clear Maven cache and rebuild
```bash
mvn clean
rm -rf ~/.m2/repository
mvn clean install
```

### Issue: Java Version Mismatch

**Symptoms**: `Unsupported class file major version` error

**Solution**: Ensure Java 21 is being used
```bash
# Check current Java version
java -version

# Set JAVA_HOME (adjust path to your Java 21 installation)
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-21

# macOS/Linux
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
```

### Issue: Tests Failing

**Solution**: Skip tests during build
```bash
mvn clean install -DskipTests
```

Then investigate test failures:
```bash
mvn test
```

### Issue: Docker Build Fails

**Solution 1**: Increase Docker memory
- Docker Desktop → Settings → Resources → Memory (set to 4GB+)

**Solution 2**: Build without cache
```bash
docker build --no-cache -t llm-token-optimizer .
```

## 📊 Verify Everything Works

Run this comprehensive test:

```bash
# 1. Check health
curl http://localhost:8080/actuator/health

# 2. Test token counting
curl -X POST http://localhost:8080/api/v1/tokens/count \
  -H "Content-Type: application/json" \
  -d '{"text":"Test"}'

# 3. Test optimization
curl -X POST http://localhost:8080/api/v1/optimize \
  -H "Content-Type: application/json" \
  -d '{"document":"Test document","contextWindow":8000}'

# 4. Check Swagger UI
# Open: http://localhost:8080/swagger-ui.html
```

If all tests pass, you're ready to go! 🎉

## 🎓 Next Steps

- Read the [README.md](README.md) for API documentation
- Explore the [Swagger UI](http://localhost:8080/swagger-ui.html)
- Check out [CONTRIBUTING.md](CONTRIBUTING.md) if you want to contribute
- Try the interactive web UI at [http://localhost:8080](http://localhost:8080)

## 💡 Tips for Development

1. **Use an IDE**: IntelliJ IDEA or Eclipse for better development experience
2. **Enable hot reload**: Use Spring Boot DevTools for automatic restarts
3. **Check logs**: Look at console output for debugging
4. **Use Postman**: For easier API testing
5. **Read Swagger docs**: Comprehensive API documentation

## 📞 Need Help?

If you're still having issues:

1. Check [GitHub Issues](https://github.com/yourusername/demo-for-llm/issues)
2. Review the [README.md](README.md)
3. Open a new issue with:
   - Your OS and Java version
   - Error messages
   - Steps you've tried

Happy coding! 🚀
