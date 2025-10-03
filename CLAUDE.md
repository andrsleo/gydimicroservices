# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GYDI Microservices - Spring Boot 3.5.5 application for affiliate rentals, built with Java 21 and Maven.

## Build & Run Commands

```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClassNameTest

# Run a specific test method
./mvnw test -Dtest=ClassNameTest#methodName
```

## Architecture

This project follows **Hexagonal Architecture** (Ports & Adapters) pattern:

### Layer Structure (within `src/main/java/com/affiliate/rentals/<bounded-context>/`)

```
domain/                    # Core business logic (framework-agnostic)
  ├─ model/               # Entities, value objects, domain models
  └─ port/                # Port interfaces (e.g., PropertyRepositoryPort)

application/               # Use cases & orchestration
  ├─ dto/                 # DTOs (use Java records for immutability)
  ├─ usecase/             # Use case interfaces and implementations
  └─ mapper/              # Domain <-> DTO mappings

adapters/                  # Infrastructure implementations
  ├─ in/                  # Inbound adapters (REST controllers, gRPC, listeners)
  │   └─ rest/
  └─ out/                 # Outbound adapters (persistence, external clients)
      ├─ persistence/     # Spring Data JPA repositories, DB entities
      └─ client/          # REST clients, event producers

config/                    # Spring configuration (beans, OpenAPI, security)

crosscutting/              # Cross-cutting concerns (logging, aspects)

common/                    # Shared utilities (exceptions, web handlers)
```

### Key Architectural Principles

- **Domain layer** is completely isolated from infrastructure (no Spring, JPA, or framework annotations)
- **Ports** define interfaces in the domain layer (e.g., `PropertyRepositoryPort`)
- **Adapters** implement ports using infrastructure (e.g., `PropertyRepositoryAdapter` implements `PropertyRepositoryPort` using Spring Data JPA)
- **Use cases** in the application layer orchestrate domain logic and coordinate between ports
- **DTOs** transfer data across boundaries and are separate from domain models
- **Mappers** translate between domain models and DTOs/entities (using MapStruct)

## Database

- **PostgreSQL** for relational data (JPA/Hibernate)
- **MongoDB** for NoSQL data (configured but usage depends on bounded context)
- **Flyway** for database migrations (located in `src/main/resources/db/migration/`)
- Database configuration in `src/main/resources/application.yml`

## Technology Stack

- Java 21 with virtual threads (see `VirtualThreadsConfig`)
- Spring Boot 3.5.5 (Web, Data JPA, Data MongoDB, Validation)
- PostgreSQL + Flyway migrations
- Lombok for boilerplate reduction
- MapStruct for object mapping
- SpringDoc OpenAPI 2.1.0 for API documentation
- Hibernate Types for JSONB support
- Spring AOP for cross-cutting concerns (see `LoggingAspect`)
- Caching configured (see `CacheConfig`)

## Development Notes

- MapStruct processors are configured with Lombok in the Maven compiler plugin
- Hibernate DDL auto is disabled (`ddl-auto: none`) - use Flyway for schema changes
- API documentation available at `/swagger-ui.html` when running
- Virtual threads are enabled for improved concurrency
