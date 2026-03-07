# java-ai-simple-demos

Simple Demos for Java Spring Boot with Spring AI and LangChain4j

## Projects

### 1. demo-spring-ai
- **Description**: AI Demo project for Spring Boot with Spring AI
- **Java Version**: 17
- **Spring Boot Version**: 3.5.6
- **Key Dependencies**:
  - Spring AI (v1.0.2)
  - Spring Boot Actuator
  - Ollama Model Support
  - Chat Memory Support
- **Purpose**: Demonstrates Spring AI integration with language models using Ollama

### 2. demo-sdd-spring
- **Description**: Domain-Driven Design (SDD) project for Spring Boot
- **Java Version**: 21
- **Spring Boot Version**: 3.5.10
- **Key Dependencies**:
  - Spring Data JPA
  - Spring Web
  - Thymeleaf
  - Flyway (Database Migrations)
  - H2 Database
- **Purpose**: Demonstrates Spring Boot application with domain-driven design patterns and database management

### 3. demo-sdd-workflow-spring
- **Description**: Lightweight embedded workflow engine for Spring Boot
- **Java Version**: 21
- **Spring Boot Version**: 3.5.11
- **Key Dependencies**:
  - Spring Data JPA
  - Spring Web
  - Thymeleaf
  - Flyway (Database Migrations)
  - H2 Database
  - SpringDoc OpenAPI / Swagger UI
- **Purpose**: Demonstrates a YAML-driven state machine workflow engine with a REST API and browser-based UI, built entirely on standard Spring Boot components

## Getting Started

Each project is a standalone Maven project. To build and run:

```bash
cd [project-directory]
mvn clean install
mvn spring-boot:run
```

## Prerequisites
- Java 21+ (Java 17 is sufficient for demo-spring-ai only)
- Maven 3.6+
