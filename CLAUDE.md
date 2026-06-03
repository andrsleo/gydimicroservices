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

## Quick Start

Use the included script for quick setup:
```bash
./start.sh
```

Or manually:
```bash
./mvnw clean install && ./mvnw spring-boot:run
```

## Test Coverage

✅ **62 tests - 100% passing**
- All functional tests pass
- Application context loads successfully
- All use cases tested

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

---

## Cómo Usar Claude Code (Skills)

Los Agent Skills se activan **automáticamente** — no requieren invocación manual con `/nombre`. Basta con describir lo que necesitas en lenguaje natural.

| Skill | Se activa cuando... |
|-------|---------------------|
| `superpowers` | Feature compleja — propone plan completo antes de escribir código |
| `test-driven-development` | Implementar domain service, use case, o cualquier lógica nueva |
| `systematic-debugging` | Test fallido, excepción, compilación rota, comportamiento inesperado |
| `software-architecture` | Validar diseño contra Hexagonal Architecture + SOLID |
| `bounded-context` | Crear nuevo dominio, servicio, o bounded context backend |
| `db-migrate` | Cambio de schema, nueva tabla, columna, constraint |
| `api-types` | Sincronizar tipos TypeScript desde DTOs Java |
| `test-feature` | Generar tests para un bounded context sin cobertura |
| `arch-check` | Antes de PR, validar arquitectura hexagonal |
| `stack-review` | Revisión de salud del proyecto backend |
| `find-skills` | "¿hay un skill para X?", descubrir capacidades disponibles |

---

## TDD en Spring Boot (JUnit 5 + Mockito)

**Regla:** `SIN CÓDIGO DE PRODUCCIÓN SIN UN TEST FALLANDO PRIMERO`

### Ciclo para Domain Service

```bash
# 1. Crear el test (debe FALLAR)
# src/test/java/.../domain/service/NombreServiceTest.java

# 2. Verificar que falla
./mvnw test -Dtest=NombreServiceTest

# 3. Implementar (mínimo para pasar)
# src/main/java/.../domain/service/NombreService.java

# 4. Verificar que pasa
./mvnw test -Dtest=NombreServiceTest

# 5. Refactorizar manteniendo verde
./mvnw test
```

### Estructura de Test Obligatoria

```java
@ExtendWith(MockitoExtension.class)
class NombreServiceTest {

    @Mock
    private NombreRepositoryPort repositoryPort; // Mock del port, nunca de JPA

    @InjectMocks
    private NombreService service;

    @Test
    @DisplayName("debe [comportamiento] cuando [condición]")
    void debe_comportamiento_cuando_condicion() {
        // Arrange — preparar datos
        // Act — ejecutar el método
        // Assert — verificar resultado
        verify(repositoryPort).metodoEsperado(any());
    }
}
```

**Reglas:**
- Tests de domain service: JUnit 5 puro (sin Spring, `@ExtendWith(MockitoExtension.class)`)
- Tests de use case: Mockito para todos los ports y el domain service
- Tests de controller: `@WebMvcTest` solo cuando sea necesario
- **NUNCA** mockear JPA directamente — usar el port como abstracción

El skill `test-driven-development` se activa automáticamente al pedir implementar algo.

---

## Debugging Sistemático

Ante cualquier fallo, el skill `systematic-debugging` se activa automáticamente. Protocolo:

```bash
# 1. Leer el error completo (nunca saltarse el stack trace)
./mvnw test 2>&1 | grep -A 20 "FAILED\|ERROR\|Exception"

# 2. Reproducir de forma confiable
./mvnw test -Dtest=ClaseTest#testMetodo

# 3. Revisar cambios recientes
git diff HEAD~3 -- src/main/java/

# 4. Recopilar evidencia (logging temporal)
log.debug("[Input] {}", request);
log.debug("[Domain result] {}", domainResult);
log.debug("[Mapped] {}", dto);

# 5. Formar UNA hipótesis y probarla con UN solo cambio mínimo
```

**Nunca:** Hacer múltiples cambios a la vez ni proponer fixes sin haber entendido la causa raíz.

---

## Reglas de Arquitectura Hexagonal (Verificación Rápida)

Antes de hacer commit, verificar:

```bash
# ¿El dominio importa Spring o JPA?
grep -r "import org.springframework" src/main/java/*/domain/ 2>/dev/null && echo "⚠️ VIOLACIÓN"
grep -r "import jakarta.persistence" src/main/java/*/domain/ 2>/dev/null && echo "⚠️ VIOLACIÓN"

# ¿Lógica de negocio en adapters?
# Revisar manualmente: los controllers deben solo delegar al use case

# ¿Use cases acceden a JPA directamente?
grep -r "JpaRepository\|EntityManager" src/main/java/*/application/ 2>/dev/null && echo "⚠️ VIOLACIÓN"
```

Mencionar "revisar arquitectura" o "auditar" activa el skill `arch-check` para un reporte completo.

---

## Patrones Obligatorios

| Componente | Regla |
|------------|-------|
| Domain model | Java puro — sin `@Entity`, sin `@Component`, sin Lombok en campos |
| DTOs | Java records con `@Valid` y Bean Validation |
| Mappers | Interfaces MapStruct — `@Mapper(componentModel = "spring")` |
| Use cases | Una responsabilidad — implementan el input port |
| Controllers | Solo delegan — sin lógica de negocio |
| Repository adapters | Implementan el output port usando JpaRepository |
| Excepciones de dominio | Extienden `RuntimeException` — en `domain/exception/` |
| Migraciones Flyway | Nomenclatura `V{n}__{descripcion_snake_case}.sql` — nunca modificar aplicadas |
