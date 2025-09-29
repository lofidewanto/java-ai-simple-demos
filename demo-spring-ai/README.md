# Customer Agent Service with Ollama LLM

This Spring Boot application provides a Customer Agent Service that integrates with Ollama LLM to provide intelligent customer service capabilities.

## Features

- **Personalized Customer Responses**: Generate contextual responses based on customer information
- **Sentiment Analysis**: Analyze customer message sentiment (POSITIVE, NEGATIVE, NEUTRAL, URGENT)
- **Product Recommendations**: Generate personalized product suggestions
- **Follow-up Messages**: Create professional follow-up communications
- **General Chat**: Open-ended chat capabilities with the LLM

## Prerequisites

1. **Java 17+**
2. **Maven 3.6+**
3. **Ollama** installed and running locally
4. **Ollama model** (e.g., llama3.2) downloaded

## Setup

### 1. Install and Start Ollama

```bash
# Install Ollama (macOS)
brew install ollama

# Start Ollama service
ollama serve

# Pull a model (in another terminal)
ollama pull llama3.2
```

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

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Health Check
```
GET /api/customer-agent/health
```

### Get Demo Customer
```
GET /api/customer-agent/demo-customer
```

### Generate Customer Response
```
POST /api/customer-agent/respond
Content-Type: application/json

{
  "customer": {
    "id": "CUST001",
    "name": "John Doe",
    "email": "john.doe@email.com",
    "phoneNumber": "+1-555-0123",
    "preferredLanguage": "English",
    "customerSegment": "Premium"
  },
  "inquiry": "I'm having trouble with my recent order"
}
```

### Sentiment Analysis
```
POST /api/customer-agent/sentiment
Content-Type: application/json

{
  "message": "I'm very frustrated with the delayed delivery!"
}
```

### Product Recommendations
```
POST /api/customer-agent/recommendations
Content-Type: application/json

{
  "customer": {
    "id": "CUST001",
    "name": "John Doe",
    "customerSegment": "Premium",
    "preferredLanguage": "English"
  },
  "context": "Customer is interested in upgrading their current plan"
}
```

### Generate Follow-up
```
POST /api/customer-agent/followup
Content-Type: application/json

{
  "customer": {
    "id": "CUST001",
    "name": "John Doe",
    "customerSegment": "Premium"
  },
  "previousInteraction": "Helped customer with billing inquiry"
}
```

### General Chat
```
POST /api/customer-agent/chat
Content-Type: application/json

{
  "systemMessage": "You are a helpful customer service agent",
  "userMessage": "How can I reset my password?"
}
```

## Usage Examples

### cURL Examples

1. **Test service health:**
```bash
curl http://localhost:8080/api/customer-agent/health
```

2. **Get demo customer:**
```bash
curl http://localhost:8080/api/customer-agent/demo-customer
```

3. **Generate customer response:**
```bash
curl -X POST http://localhost:8080/api/customer-agent/respond \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "id": "CUST001",
      "name": "Jane Smith",
      "email": "jane.smith@email.com",
      "phoneNumber": "+1-555-0456",
      "preferredLanguage": "English",
      "customerSegment": "Premium"
    },
    "inquiry": "I need help setting up my new account"
  }'
```

4. **Analyze sentiment:**
```bash
curl -X POST http://localhost:8080/api/customer-agent/sentiment \
  -H "Content-Type: application/json" \
  -d '{"message": "Thank you so much for the excellent service!"}'
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/demo/
│   │       ├── config/
│   │       │   └── OllamaConfig.java
│   │       ├── controller/
│   │       │   └── CustomerAgentController.java
│   │       ├── model/
│   │       │   └── Customer.java
│   │       ├── service/
│   │       │   └── CustomerAgentService.java
│   │       └── DemoAiApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/
        └── com/example/demo/
            └── service/
                └── CustomerAgentServiceTest.java
```

## Key Classes

- **CustomerAgentService**: Core service containing LLM integration logic
- **CustomerAgentController**: REST API endpoints
- **Customer**: Model representing customer data
- **OllamaConfig**: Configuration for Ollama integration

## Troubleshooting

1. **"Connection refused" errors**: Ensure Ollama is running (`ollama serve`)
2. **"Model not found" errors**: Pull the required model (`ollama pull llama3.2`)
3. **Slow responses**: The model might be loading; wait a moment and try again
4. **Memory issues**: Ensure your system has sufficient RAM for the Ollama model

## Extending the Service

You can extend the `CustomerAgentService` by:

1. Adding new methods for specific customer service scenarios
2. Implementing conversation memory for multi-turn dialogues
3. Adding integration with external customer databases
4. Implementing different prompt templates for various business domains
5. Adding support for multiple languages

## Testing

Run the tests with:
```bash
mvn test
```

The tests include basic unit tests for the Customer model and service instantiation.