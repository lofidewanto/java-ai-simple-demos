# Guest Book - Spring Boot Web Application

A modern, full-featured Guest Book web application built with Spring Boot 3.5.10, featuring both a web interface and REST API with Swagger UI documentation.

## Features

- **Web UI**: View, add, edit, and delete guest book entries with a Bootstrap-styled interface
- **REST API**: Full CRUD operations with JSON responses
- **API Documentation**: Interactive Swagger UI for testing endpoints
- **Database**: H2 in-memory database with Flyway migrations
- **Testing**: Comprehensive unit and integration tests

## Technologies

- **Spring Boot 3.5.10** - Application framework
- **Java 21** - Programming language
- **Spring Data JPA** - Data access layer
- **Thymeleaf** - Server-side templating
- **Bootstrap 5** - Frontend styling
- **H2 Database** - In-memory database
- **Flyway** - Database migration tool
- **Springdoc OpenAPI** - API documentation
- **JUnit 5** - Testing framework

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6+

### Installation

1. Clone the repository:
```bash
git clone https://github.com/lofidewanto/java-ai-simple-demos.git
cd java-ai-simple-demos/demo-sdd-spring
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Usage

### Web Interface

Access the guest book web interface at:
```
http://localhost:8080/
```

Features:
- **View Entries**: See all guest book entries (newest first)
- **Add Entry**: Click "Add Entry" button to create a new entry
- **Edit Entry**: Click "Edit" button on any entry card
- **Delete Entry**: Click "Delete" button (with confirmation)

### REST API

Base URL: `http://localhost:8080/api/entries`

Endpoints:
- `GET /api/entries` - Get all entries
- `GET /api/entries/{id}` - Get entry by ID
- `POST /api/entries` - Create new entry
- `PUT /api/entries/{id}` - Update entry
- `DELETE /api/entries/{id}` - Delete entry

Example using curl:
```bash
# Get all entries
curl http://localhost:8080/api/entries

# Create new entry
curl -X POST http://localhost:8080/api/entries \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","message":"Great site!"}'

# Update entry
curl -X PUT http://localhost:8080/api/entries/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"John Updated","email":"john@example.com","message":"Updated message"}'

# Delete entry
curl -X DELETE http://localhost:8080/api/entries/1
```

### Swagger UI (API Documentation)

Interactive API documentation available at:
```
http://localhost:8080/swagger-ui.html
```

Features:
- Browse all available endpoints
- Test API operations directly from the browser
- View request/response schemas
- Download OpenAPI specification

OpenAPI specification files:
- JSON: `http://localhost:8080/api-docs`
- YAML: `http://localhost:8080/api-docs.yaml`

### H2 Database Console

Access the H2 database console at:
```
http://localhost:8080/h2-console
```

Connection settings:
- **JDBC URL**: `jdbc:h2:mem:guestbookdb`
- **Username**: `sa`
- **Password**: (leave empty)

## Running Tests

Run all tests:
```bash
mvn test
```

Run specific test class:
```bash
mvn test -Dtest=GuestBookServiceTest
```

Test Coverage:
- **Unit Tests**: Service layer business logic
- **Integration Tests**: Repository database operations
- **Controller Tests**: REST API and MVC endpoints

## Project Structure

```
demo-sdd-spring/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo_sdd_spring/
│   │   │   ├── DemoSddSpringApplication.java
│   │   │   ├── config/
│   │   │   │   └── DataInitializer.java
│   │   │   ├── controller/
│   │   │   │   ├── api/
│   │   │   │   │   └── GuestBookApiController.java
│   │   │   │   └── web/
│   │   │   │       └── GuestBookWebController.java
│   │   │   ├── model/
│   │   │   │   └── GuestBookEntry.java
│   │   │   ├── repository/
│   │   │   │   └── GuestBookEntryRepository.java
│   │   │   └── service/
│   │   │       └── GuestBookService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/migration/
│   │       │   └── V1__Create_guest_book_entry_table.sql
│   │       ├── static/
│   │       │   └── css/
│   │       │       └── style.css
│   │       └── templates/
│   │           └── entries/
│   │               ├── list.html
│   │               └── form.html
│   └── test/
│       └── java/com/example/demo_sdd_spring/
│           ├── controller/
│           │   ├── api/
│           │   │   └── GuestBookApiControllerTest.java
│           │   └── web/
│           │       └── GuestBookWebControllerTest.java
│           ├── repository/
│           │   └── GuestBookEntryRepositoryTest.java
│           └── service/
│               └── GuestBookServiceTest.java
├── pom.xml
└── README.md
```

## Database Schema

### guest_book_entry table

| Column     | Type         | Constraints           |
|------------|--------------|-----------------------|
| id         | BIGINT       | PRIMARY KEY, AUTO_INCREMENT |
| name       | VARCHAR(255) | NOT NULL              |
| email      | VARCHAR(255) | -                     |
| message    | TEXT         | NOT NULL              |
| created_at | TIMESTAMP    | NOT NULL, DEFAULT NOW |
| updated_at | TIMESTAMP    | NOT NULL, DEFAULT NOW |

## Configuration

Key configuration in `application.properties`:

```properties
# Application name
spring.application.name=demo-sdd-spring

# H2 Database
spring.datasource.url=jdbc:h2:mem:guestbookdb
spring.h2.console.enabled=true

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

# Flyway
spring.flyway.enabled=true

# OpenAPI/Swagger
springdoc.swagger-ui.path=/swagger-ui.html
```

## Sample Data

The application includes sample data initialization with 3 pre-populated entries for demonstration purposes. This can be disabled by removing or commenting out the `DataInitializer` class.

## Development

### Adding New Features

1. Create/modify entity in `model/` package
2. Update repository in `repository/` package
3. Implement business logic in `service/` package
4. Add controllers in `controller/` package
5. Create Thymeleaf templates in `templates/`
6. Write tests for new functionality

### Database Migrations

Create new migration files in `src/main/resources/db/migration/` following Flyway naming convention:
```
V{version}__{description}.sql
```

Example: `V2__Add_rating_column.sql`

## Troubleshooting

### Application won't start
- Check if port 8080 is already in use
- Verify Java 21 is installed: `java -version`
- Clean and rebuild: `mvn clean install`

### Database errors
- H2 console not accessible: Check `spring.h2.console.enabled=true`
- Migration errors: Review Flyway migration scripts
- Clear H2 database: Restart application (in-memory database resets)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is part of the java-ai-simple-demos repository.

## Author

Created as part of the Spring Boot demonstration project.

## Acknowledgments

- Spring Boot team for the excellent framework
- Bootstrap for the UI components
- Springdoc for OpenAPI integration
