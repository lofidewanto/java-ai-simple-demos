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

## Getting Started

Each project is a standalone Maven project. To build and run:

```bash
cd [project-directory]
mvn clean install
mvn spring-boot:run
```

## Prerequisites
- Java 17+ (Java 21 recommended for demo-sdd-spring)
- Maven 3.6+
