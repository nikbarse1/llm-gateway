# Contributing to Advanced LLM Gateway & Token Optimizer

Thank you for your interest in contributing! This document provides guidelines for contributing to this sophisticated LLM gateway project.

## 🎯 Ways to Contribute

- **🐛 Bug Reports**: Report bugs via GitHub Issues with detailed reproduction steps
- **💡 Feature Requests**: Suggest new features or improvements for token optimization, chat capabilities, or provider integrations
- **🔧 Code Contributions**: Submit pull requests with bug fixes or new features
- **📚 Documentation**: Improve README, API docs, code comments, or add examples
- **🧪 Testing**: Add unit tests or integration tests for better coverage
- **🔌 Provider Integrations**: Add support for new LLM providers
- **🎨 UI/UX**: Improve the web interface and user experience
- **📊 Analytics**: Enhance metrics, monitoring, and cost tracking features

## 🚀 Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/nikbarse1/demo-for-llm.git
   cd demo-for-llm
   ```
3. **Create a branch** for your changes:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## 💻 Development Setup

### Prerequisites
- **Java 21+** (required for Spring Boot 4.x)
- **Maven 3.9+** (for dependency management)
- **Git** (version control)
- **Docker** (optional, for containerized testing)
- **API Keys**:
  - Azure AI Inference or Groq API key (required)
  - Gemini API key (optional, for advanced features)
- Your favorite IDE (IntelliJ IDEA recommended)

### Initial Setup
```bash
# Clone your fork
git clone https://github.com/your-username/demo-for-llm.git
cd demo-for-llm

# Set up environment variables
cp .env.example .env
# Edit .env with your API keys

# Install dependencies and build
mvn clean install

# Run tests
mvn test

# Start the application
mvn spring-boot:run
```

### Development Workflow
```bash
# Create a feature branch
git checkout -b feature/your-feature-name

# Make your changes
# ...

# Run tests frequently
mvn test

# Build to ensure no compilation errors
mvn clean compile

# Run application locally for testing
mvn spring-boot:run

# Test APIs
curl -X POST http://localhost:8080/api/v1/tokens/count \
  -H "Content-Type: application/json" \
  -d '{"text":"test"}'
```

## 📝 Code Guidelines

### Java Code Style
- Follow standard Java naming conventions
- Use meaningful variable and method names
- Keep methods focused and concise (< 50 lines preferred)
- Add comprehensive JavaDoc for public methods and classes
- Use Lombok annotations to reduce boilerplate
- Apply reactive programming patterns with WebFlux
- Use proper exception handling with custom exceptions

### Architecture Guidelines
- **Separation of Concerns**: Keep controllers, services, and repositories distinct
- **Dependency Injection**: Use Spring's constructor injection
- **Reactive Programming**: Use Mono/Flux for async operations
- **Error Handling**: Use GlobalExceptionHandler for consistent error responses
- **Configuration**: Externalize configuration in properties files
- **Testing**: Write unit tests for services and integration tests for controllers

### Code Examples:

#### Service Layer Example:
```java
/**
 * Processes chat requests with intelligent routing and optimization.
 * 
 * @param instruction The user instruction
 * @param file Optional document file
 * @param chatId Session identifier for context
 * @return Mono containing the AI response
 */
public Mono<AiChatResponse> processChat(
        String instruction, 
        MultipartFile file, 
        String chatId,
        String provider,
        int contextWindow) {
    
    return Mono.fromCallable(() -> validateInputs(instruction, file))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(validated -> resolveDocumentContext(file))
            .flatMap(context -> orchestrateChat(instruction, context, chatId, provider, contextWindow))
            .onErrorMap(ValidationException.class, ex -> new InvalidRequestException(ex.getMessage()))
            .onErrorResume(WebClientException.class, ex -> handleProviderError(ex));
}
```

#### Controller Example:
```java
/**
 * Advanced chat endpoint supporting file uploads and URL processing.
 */
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Mono<AiChatResponse> chat(
        @RequestParam("instruction") String instruction,
        @RequestParam(value = "file", required = false) MultipartFile file,
        @RequestParam(value = "url", required = false) String url,
        @RequestParam(value = "chatId", required = false) String chatId,
        @RequestParam(value = "provider", defaultValue = "FAST_TIER") String provider,
        @RequestParam(value = "contextWindow", defaultValue = "8192") int contextWindow,
        @RequestHeader(value = "X-Developer-Mode", defaultValue = "false") boolean isDevMode) {
    
    log.info("Chat request - ChatId: {}, Provider: {}, File: {}, URL: {}", 
            chatId, provider, file != null, url != null);
    
    return gatewayOrchestrationService.processStatefulChat(
            instruction, file, url, chatId, provider, contextWindow, isDevMode);
}
```

### Testing Guidelines
- Write unit tests for all new features and services
- Aim for >80% code coverage
- Use descriptive test method names following the pattern `methodName_condition_expectedResult`
- Follow AAA pattern: Arrange, Act, Assert
- Use Mockito for mocking dependencies
- Test reactive streams with StepVerifier
- Include integration tests for API endpoints

#### Unit Test Example:
```java
@ExtendWith(MockitoExtension.class)
class TokenCounterServiceTest {
    
    @Mock
    private Encoding encoding;
    
    @InjectMocks
    private TokenCounterService tokenService;
    
    @Test
    void countTokens_withValidText_returnsTokenCount() {
        // Arrange
        String text = "Hello, world!";
        when(encoding.countTokens(text)).thenReturn(3);
        
        // Act
        TokenResponse response = tokenService.countTokens(text);
        
        // Assert
        assertEquals(3, response.getTokenCount());
        assertEquals(text, response.getOriginalText());
        verify(encoding).countTokens(text);
    }
    
    @Test
    void countTokens_withNullText_returnsZero() {
        // Act
        TokenResponse response = tokenService.countTokens(null);
        
        // Assert
        assertEquals(0, response.getTokenCount());
    }
}
```

#### Reactive Test Example:
```java
@Test
void processChat_withValidRequest_returnsResponse() {
    // Arrange
    String instruction = "Test instruction";
    String chatId = "test-chat-123";
    
    when(historyRepository.findByChatId(chatId))
            .thenReturn(Mono.just(new ChatSessionState()));
    when(providerRegistry.getProvider("FAST_TIER"))
            .thenReturn(mockProvider);
    when(mockProvider.askAi(any()))
            .thenReturn(Mono.just("Test response"));
    
    // Act & Assert
    StepVerifier.create(gatewayService.processStatefulChat(
            instruction, null, null, chatId, "FAST_TIER", 8192, false))
            .assertNext(response -> {
                assertEquals("Test response", response.getUserReadableMessage());
                assertEquals(chatId, response.getChatId());
            })
            .verifyComplete();
}
```

### Feature-Specific Guidelines

#### 🔌 Adding New LLM Providers
1. **Create Provider Implementation**:
   ```java
   @Component
   public class NewLlmProvider implements LlmProvider {
       @Override
       public Mono<String> askAi(String prompt) {
           // Implementation for new provider
       }
   }
   ```

2. **Register in ProviderRegistry**:
   ```java
   @Bean
   public LlmProviderRegistry providerRegistry() {
       return new LlmProviderRegistry(Map.of(
           "NEW_PROVIDER", new NewLlmProvider(),
           "FAST_TIER", new FastTierLlmProvider(),
           "GEMINI", new GeminiLlmProvider()
       ));
   }
   ```

3. **Add Configuration Properties**:
   ```properties
   newllm.api.key=${NEW_LLM_API_KEY:}
   newllm.base.url=https://api.newllm.com
   newllm.model=default-model
   ```

#### 📊 Adding Metrics and Analytics
1. **Create Metrics Component**:
   ```java
   @Component
   public class MetricsCollector {
       private final MeterRegistry meterRegistry;
       
       public void recordTokenUsage(String provider, int tokens) {
           Counter.builder("llm.tokens.used")
                   .tag("provider", provider)
                   .register(meterRegistry)
                   .increment(tokens);
       }
   }
   ```

2. **Add Custom Metrics Endpoints**:
   ```java
   @RestController
   @RequestMapping("/api/metrics")
   public class MetricsController {
       @GetMapping("/usage")
       public Map<String, Object> getUsageMetrics() {
           // Return custom metrics
       }
   }
   ```

#### 🧪 Testing File Upload Features
```java
@Test
void chat_withFileUpload_processesContent() throws IOException {
    // Arrange
    MockMultipartFile file = new MockMultipartFile(
            "file", "test.txt", "text/plain", "Test content".getBytes());
    
    // Act
    Mono<AiChatResponse> result = controller.chat(
            "Summarize this", file, null, "test-123", "FAST_TIER", 8192, false);
    
    // Assert
    StepVerifier.create(result)
            .assertNext(response -> assertEquals("FILE", response.getSourceType()))
            .verifyComplete();
}
```

## 🔄 Pull Request Process

1. **Update your branch** with the latest main:
   ```bash
   git checkout main
   git pull upstream main
   git checkout your-feature-branch
   git rebase main
   ```

2. **Ensure all tests pass**:
   ```bash
   mvn clean test
   ```

3. **Commit your changes** with clear messages:
   ```bash
   git commit -m "Add feature: token cost calculator"
   ```

4. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

5. **Create a Pull Request** on GitHub with:
   - Clear title describing the change
   - Detailed description of what and why
   - Reference to any related issues
   - Screenshots (if UI changes)

## 📋 Commit Message Guidelines

Use conventional commit format:

```
type(scope): subject

body (optional)

footer (optional)
```

### Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### Examples:
```
feat(api): add batch token counting endpoint

fix(summarization): handle null responses from LLM API

docs(readme): add Docker deployment instructions

test(controller): add integration tests for optimization endpoint
```

## 🐛 Reporting Bugs

When reporting bugs, please include:

1. **Description**: Clear description of the bug
2. **Steps to Reproduce**: Detailed steps to reproduce the issue
3. **Expected Behavior**: What you expected to happen
4. **Actual Behavior**: What actually happened
5. **Environment**: 
   - OS (Windows, macOS, Linux)
   - Java version
   - Spring Boot version
6. **Logs**: Relevant error messages or stack traces
7. **Screenshots**: If applicable

### Bug Report Template:
```markdown
**Description**
Brief description of the bug

**Steps to Reproduce**
1. Step one
2. Step two
3. Step three

**Expected Behavior**
What should happen

**Actual Behavior**
What actually happens

**Environment**
- OS: Windows 11
- Java: 21.0.1
- Spring Boot: 4.1.0

**Logs**
```
Error logs here
```

**Screenshots**
[Attach screenshots if applicable]
```

## 💡 Feature Requests

When suggesting features, please include:

1. **Use Case**: Why is this feature needed?
2. **Proposed Solution**: How should it work?
3. **Alternatives**: Other solutions you've considered
4. **Additional Context**: Any other relevant information

## ✅ Code Review Process

All submissions require review. We aim to:

- Review PRs within 2-3 business days
- Provide constructive feedback
- Ensure code quality and test coverage
- Maintain project consistency

## 📜 Code of Conduct

### Our Standards

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Focus on constructive criticism
- Respect differing viewpoints
- Show empathy towards others

### Unacceptable Behavior

- Harassment or discriminatory language
- Trolling or insulting comments
- Personal or political attacks
- Publishing others' private information

## 🎓 Learning Resources

If you're new to:

- **Spring Boot**: [Official Documentation](https://spring.io/projects/spring-boot)
- **Java**: [Oracle Java Tutorials](https://docs.oracle.com/javase/tutorial/)
- **Maven**: [Maven Getting Started](https://maven.apache.org/guides/getting-started/)
- **Git**: [Git Handbook](https://guides.github.com/introduction/git-handbook/)

## 🙋 Questions?

If you have questions:

1. Check existing [Issues](https://github.com/yourusername/demo-for-llm/issues)
2. Check the [README](README.md)
3. Open a new issue with the `question` label

## 📞 Contact

For urgent matters or private concerns, please contact the maintainers directly.

---

Thank you for contributing to LLM Token Optimizer! 🎉
