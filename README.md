# GYDI — Vacation Rentals & Affiliate Platform (Backend)

Backend for **GYDI 2.0**, a platform for vacation property sales and rentals with a referral-commission system. Built as a **modular monolith with Hexagonal Architecture** (Ports & Adapters), organized by bounded contexts and designed to be split into microservices as the product scales.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)

## Architecture

Each bounded context (e.g. bookings, properties, referrals) follows Hexagonal Architecture:

```
com.affiliate.rentals.gydi.<context>/
├── domain/          # Pure business logic — no framework dependencies
│   ├── model/       # Aggregates, entities, value objects (Money, BookingDates, GuestInfo)
│   ├── event/       # Domain events (BookingCreatedEvent, BookingFinishedEvent...)
│   ├── exception/   # Business exceptions
│   └── ports/       # Repository interfaces (driven ports)
├── application/     # Use cases — one class per operation
│   ├── usecase/     # CreateBookingUseCase, CancelBookingUseCase...
│   ├── dto/         # Request/response objects
│   └── mapper/      # MapStruct mappers
└── adapter/         # Infrastructure: REST controllers, JPA repositories
```

**Key design decisions**

- **Use case per class**: each operation (reserve, cancel, dispute, finish) is an isolated, testable unit.
- **Domain events** decouple contexts — e.g. a finished booking triggers commission calculation without direct coupling.
- **Value objects** (Money, BookingDates) enforce invariants at the type level.
- **Booking state machine** with explicit status transitions and InvalidBookingStatusTransitionException guarding illegal moves.
- **CSRF protection** implemented and documented in docs/security/.

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Framework | Java 21, Spring Boot 3.5 |
| Persistence | PostgreSQL, Spring Data JPA, Flyway migrations |
| Security | Spring Security + JWT (jjwt) |
| Cloud | Spring Cloud AWS |
| Email | Brevo (transactional) |
| Observability | Spring Boot Actuator |
| Mapping | MapStruct, Lombok |
| Testing | Spring Boot Test, H2 |

## Getting Started

```bash
# Prerequisites: Java 21, Maven, PostgreSQL (or Docker)

git clone https://github.com/andrsleo/gydimicroservices.git
cd gydimicroservices

# Configure database & secrets
export DB_URL=jdbc:postgresql://localhost:5432/gydi
export DB_USER=postgres
export DB_PASSWORD=<password>
export JWT_SECRET=<secret>

# Run (Flyway applies migrations on startup)
./mvnw spring-boot:run
```

Or with Docker:

```bash
docker build -t gydi-backend .
docker run -p 8080:8080 --env-file .env gydi-backend
```

## Testing

```bash
./mvnw test
```

## Related Repositories

- [GydiFrontNext](https://github.com/andrsleo/GydiFrontNext) — Next.js 15 frontend (current)

---
Built by [Andrés Vargas](https://github.com/andrsleo) · Property of GYDI
