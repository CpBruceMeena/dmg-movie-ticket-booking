# Agents.md — Project Development Workflow

> This document defines the development agents, workflow, and collaboration model for the DMG Movie Ticket Booking System.

---

## Overview

This project follows a structured development methodology that separates concerns across multiple agents and development phases. Each agent has a well-defined responsibility, and all changes flow through a consistent pipeline from requirements to deployment.

---

## Development Agents & Roles

| Agent | Role | Responsibility |
|-------|------|---------------|
| **Project Lead** | Architecture & Orchestration | Defines the overall architecture, breaks down epics into tasks, orchestrates development across all agents, and makes final technical decisions. |
| **Backend Developer** | Java / Spring Boot Implementation | Implements REST APIs, service layers, JPA entities, repositories, and business logic. Ensures code follows Java conventions and Spring Boot best practices. |
| **Database Modeler** | Data Layer Design | Designs entity relationships, JPA mappings, indexing strategy, and query optimization. Ensures data integrity and concurrency safety. |
| **API Designer** | REST API Contracts | Defines endpoint contracts, request/response DTOs, validation rules, HTTP status codes, and error response formats. Documents APIs via OpenAPI/Swagger. |
| **Security Engineer** | Auth & Authorization | Configures Spring Security, role-based access control (RBAC), endpoint protection rules, and user authentication. |
| **QA Engineer** | Testing & Validation | Writes unit tests (JUnit 5), integration tests (`@SpringBootTest`), and validates edge cases, concurrency scenarios, and error handling. |
| **Code Reviewer** | Quality Gate | Reviews all pull requests for correctness, code style, test coverage, edge cases, and adherence to project conventions before merge. |
| **Documenter** | Technical Writing | Maintains README, API documentation, inline code comments, and development workflow documentation. |

---

## Development Workflow

### Phase 1: Requirements & Planning

1. **Requirement Analysis**: Read the specification (PDF) to extract functional and non-functional requirements.
2. **Scoping**: Identify in-scope vs out-of-scope features. Document assumptions.
3. **Task Breakdown**: Decompose the work into ordered, independent tasks using a todo-based tracker.
4. **Architecture Review**: Define the entity model, API contracts, and technology choices before implementation begins.

**Deliverable**: Approved task plan with documented assumptions and architectural decisions.

### Phase 2: Implementation

1. **Branch Strategy**: All work-in-progress changes must be made on **feature branches** branched off `main`.
2. **Commit Convention**: Use descriptive commit messages following a `type: description` format:

   ```
   feat: add city management endpoints
   fix: correct seat hold expiry calculation
   docs: update API documentation with Swagger annotations
   test: add unit tests for booking service
   refactor: extract pricing calculation into separate service
   ```

3. **Code Standards**:
   - Follow **Java 17** language features (records, sealed classes, pattern matching where appropriate).
   - Use **Spring Boot 3.2** idioms: constructor injection, `@Service`/`@Repository`/`@Controller` annotations, `application.properties` for config.
   - Write **self-documenting code**: meaningful names, avoid magic numbers, extract complex logic into well-named methods.
   - Include **OpenAPI/Swagger annotations** on all REST controllers for auto-generated API documentation.

4. **Change Scoping**: Each commit should be scoped to a single logical change. Avoid mixing unrelated changes in the same commit.

**Deliverable**: Working, compilable code on a feature branch.

### Phase 3: Review & Quality Gates

1. **Self-Review**: Before requesting a review, verify:
   - Code compiles (`mvn compile`)
   - All existing tests pass (`mvn test`)
   - New code has adequate test coverage
   - No debug logs, TODO comments, or dead code remain

2. **Peer Review**:
   - Reviewer checks correctness, edge cases, style, and test coverage.
   - All review comments must be addressed before merge.
   - If the reviewer approves, the branch is merged via pull request into `main`.

3. **Merge Requirements**:
   - Pull request must target `main`.
   - All conversations must be resolved.
   - No direct commits or pushes to `main`.

**Deliverable**: Reviewed, tested, and merged code into `main`.

### Phase 4: Documentation & Retrospective

1. **API Documentation**: Swagger UI is available at `/swagger-ui.html` after application startup.
2. **README Updates**: Keep the README current with new endpoints, configuration changes, and assumptions.
3. **Retrospective**: Log lessons learned and update the development workflow as needed.

**Deliverable**: Updated documentation and workflow improvements.

---

## Project Conventions

### Naming Conventions

| Artifact | Convention | Example |
|----------|-----------|---------|
| Packages | `com.dmg.moviebooking.<layer>` | `com.dmg.moviebooking.service` |
| Classes | PascalCase | `BookingService`, `CityController` |
| Methods | camelCase | `createBooking()`, `findAvailableSeats()` |
| Constants | UPPER_SNAKE_CASE | `MAX_SEAT_HOLD_MINUTES` |
| Database Tables | snake_case | `cities`, `theaters`, `seat_bookings` |

### Layered Architecture

```
Controller → Service → Repository → Database
     ↓
    DTOs
```

- **Controllers**: Handle HTTP requests/responses, validation, and Swagger annotations.
- **Services**: Business logic, transaction management, and cross-cutting concerns.
- **Repositories**: Database access via Spring Data JPA.
- **Entities**: JPA-mapped domain objects with lifecycle callbacks.

### Dependency Injection

Use **constructor injection** exclusively. Declare `final` fields for all injected dependencies.

```java
@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;

    public BookingService(BookingRepository bookingRepository, PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.paymentService = paymentService;
    }
}
```

---

## Technology Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 17 (LTS) | Core language |
| Spring Boot | 3.2.0 | Application framework |
| Spring Data JPA | — | ORM & database access |
| Spring Security | — | Authentication & RBAC |
| H2 Database | — | In-memory development database |
| Lombok | — | Boilerplate reduction |
| SpringDoc OpenAPI | 2.3.0 | API documentation (Swagger UI) |
| JUnit 5 + Mockito | — | Testing |
| Maven | 3.8+ | Build & dependency management |

---

*Last updated: July 16, 2026*
