# Customer Agent Service with Ollama LLM

This Spring Boot application provides a Customer Agent Service that integrates with Ollama LLM to provide intelligent customer service capabilities.

## Features

- **Personalized Customer Responses**: Generate contextual responses based on customer information using Spring AI
- **Tool Integration**: Uses Spring AI's @Tool annotation to fetch customer data dynamically
- **Immutable Customer Data**: Uses Java records for thread-safe customer information handling
- **Comprehensive Logging**: Detailed logging of LLM interactions and timing metrics
- **Error Handling**: Robust error handling for LLM communication failures

## Prerequisites

1. **Java 21+**
2. **Maven 3.6+**
3. **Ollama** installed and running locally
4. **Ollama model** (e.g., llama3.2) downloaded

## Setup

### 2. Configure Application

The application is pre-configured to use:
- Ollama URL: `http://localhost:11434`
- Model: `llama3.2`
- Temperature: `0.7`
- Top-p: `0.9`

You can modify these settings in `src/main/resources/application.properties`:

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=llama3.2
spring.ai.ollama.chat.options.temperature=0.7
spring.ai.ollama.chat.options.top-p=0.9
```
