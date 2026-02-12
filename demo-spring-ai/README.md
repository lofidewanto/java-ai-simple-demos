# Customer Agent Service with Ollama LLM

A Spring Boot application that demonstrates intelligent customer service capabilities by integrating Spring AI with Ollama LLM. This project showcases how to build AI-powered agents using Spring Framework's latest AI integration features.

## Features

- **AI-Powered Response Generation**: Generate intelligent, contextual customer responses using Ollama LLM models
- **Spring AI Tool Integration**: Uses Spring AI's `@Tool` annotation to enable the LLM to dynamically fetch customer data
- **Prompt Engineering**: Configurable system and user prompts with support for agent personas (e.g., Amanda from "LoFi Tech Solutions")
- **Immutable Data Models**: Leverages Java 17 records for thread-safe customer information handling
- **Comprehensive Logging**: Multi-level logging (INFO, DEBUG, TRACE) for detailed visibility into LLM interactions and performance metrics
- **Robust Error Handling**: Graceful degradation and detailed error diagnostics for LLM communication failures
- **Integration Testing**: Functional tests validating the complete AI service workflow

## Project Structure

```
demo-spring-ai/
├── src/main/java/com/example/demo/
│   ├── DemoAiApplication.java          # Application entry point with ChatClient bean
│   ├── CustomerAgentService.java       # Service layer for AI orchestration
│   ├── CustomerRepository.java         # Repository with Spring AI @Tool integration
│   └── Customer.java                   # Immutable customer data model (Java record)
├── src/main/resources/
│   ├── application.properties           # Configuration for Ollama and LLM parameters
│   └── agent-persona.txt                # Pre-defined agent personas
├── src/test/java/com/example/demo/
│   └── CustomerAgentServiceTest.java   # Integration tests
└── specs/                               # Architecture and design documentation
```

## Prerequisites

1. **Java 17+** (Java 21+ recommended)
2. **Maven 3.6+**
3. **Ollama** installed and running on `http://localhost:11434`
4. **Ollama Model**: `gpt-oss:20b` downloaded and available

   To download the model:
   ```bash
   ollama pull gpt-oss:20b
   ```

## Setup & Configuration

### 1. Start Ollama

Ensure Ollama is running with the required model:

```bash
ollama serve
# In another terminal, verify the model is available:
ollama list
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run Tests

```bash
mvn test
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

### 5. Configuration

The application is pre-configured with sensible defaults in `src/main/resources/application.properties`:

```properties
# Application Configuration
spring.application.name=demo-spriai

# Ollama LLM Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=gpt-oss:20b
spring.ai.ollama.chat.options.temperature=0.7
spring.ai.ollama.chat.options.top-k=40
spring.ai.ollama.chat.options.top-p=0.9
spring.ai.ollama.chat.options.num-predict=10000

# Logging Configuration
logging.level.com.example.demo=debug
logging.level.org.springframework.ai=debug
logging.level.org.springframework.ai.chat=trace
logging.level.org.springframework.ai.ollama=trace
```

You can customize these settings as needed. Key parameters:

- **model**: The Ollama model to use (e.g., `gpt-oss:20b`, `llama3.2`, `mistral`)
- **temperature**: Controls creativity/randomness (0.0-1.0, lower = more deterministic)
- **top-k**: Restricts sampling to top-K tokens
- **top-p**: Nucleus sampling parameter (0.0-1.0)
- **num-predict**: Maximum tokens in the response

## Usage

### Service Method

Directly use the `CustomerAgentService` in your application:

```java
@Autowired
private CustomerAgentService customerAgentService;

public void handleCustomerInquiry() {
    String response = customerAgentService.generateCustomerResponse(
        "CUST001",
        "What are your business hours?"
    );
    System.out.println(response);
}
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CustomerAgentServiceTest
```

The `CustomerAgentServiceTest` validates that the service correctly generates responses using the LLM and handles customer data appropriately.

## Architecture

### Key Components

1. **DemoAiApplication**: Initializes the Spring Boot application and creates the `ChatClient` bean for LLM communication

2. **CustomerAgentService**: Orchestrates AI interactions by:
   - Managing prompt templates (system and user prompts)
   - Invoking the ChatClient with LLM requests
   - Tracking performance metrics and logging
   - Handling errors gracefully

3. **CustomerRepository**: Provides customer data to the LLM via Spring AI's `@Tool` annotation:
   - Enables the LLM to dynamically fetch customer information during conversations
   - Implements `getCustomerById(String customerId): Customer` as a callable tool

4. **Customer**: Immutable data model (Java 17 record) representing customer information

### Data Model (Customer Record)

```java
public record Customer(
    String id,
    String name,
    String email,
    String phoneNumber,
    String preferredLanguage,
    String lastInteraction,
    String customerSegment
) {}
```

## Dependencies

- **Spring Boot 3.5.6**: Application framework
- **Spring AI 1.0.2**: LLM integration framework
- **Ollama**: Local LLM runtime (required external service)
- **Java 17+**: Programming language
- **Maven 3.6+**: Build tool

## Documentation

For detailed architecture and design information, see the `specs/` directory:
- `specs/ARCHITECTURE.md`: Detailed system architecture
- `specs/PROJECT_OVERVIEW.md`: Comprehensive project documentation
- `specs/README.md`: Specifications index

## Future Enhancements

- REST API endpoints for service access
- Database integration for persistent customer data
- Authentication and authorization
- Chat memory for multi-turn conversations
- Support for additional LLM providers
- Production deployment configurations

## Troubleshooting

### Ollama Connection Issues

If you see connection errors:
- Verify Ollama is running: `curl http://localhost:11434/api/version`
- Check the configured URL matches your Ollama setup
- Ensure the model is downloaded: `ollama list`

### Model Loading Issues

If the model fails to load:
- Pull the model manually: `ollama pull gpt-oss:20b`
- Check available disk space (20B model requires ~15GB)
- Review Ollama logs for detailed error messages

### Memory Issues

The `gpt-oss:20b` model requires significant memory:
- Ollama: 8-16GB RAM (16GB recommended)
- Spring Boot JVM: 512MB-2GB heap
- GPU acceleration recommended for faster inference

## License

This project is part of the java-ai-simple-demos repository.
