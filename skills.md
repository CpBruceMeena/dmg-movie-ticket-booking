# Skills.md — Java & Spring Boot Development Skills

> This document catalogs the technical skills, tools, and practices required for the DMG Movie Ticket Booking System development.

---

## Technology Stack

| Technology | Version |
|-----------|---------|
| **Java** | 17 (LTS) |
| **Spring Boot** | 3.2.0 |
| **Maven** | 3.8+ |
| **H2 Database** | (in-memory) |
| **SpringDoc OpenAPI** | 2.3.0 |

---

## Core Java Skills (Java 17)

### Language Features
- **Records**: Use for immutable DTOs and data carriers.
  ```java
  public record BookingResponse(Long id, String status, List<SeatInfo> seats) {}
  ```
- **Sealed Classes**: Use for constrained domain hierarchies like booking states or seat types.
- **Pattern Matching for `instanceof`**: Simplify type checks in equals/hashCode or visitor patterns.
- **Text Blocks**: Use for multi-line SQL queries, JSON templates, or rich log messages.
- **Switch Expressions**: Prefer over traditional switch statements for enum-based logic.
- **Streams & Optional**: Use declaratively for collection processing and nullable values.

### Exception Handling
- Define domain-specific exceptions extending `RuntimeException`.
- Use `@ControllerAdvice` with `@ExceptionHandler` for consistent error responses.
- Never swallow exceptions — always log and re-throw or translate appropriately.

---

## Spring Boot Skills

### Project Setup & Configuration
- Use `spring-boot-starter-parent` as the Maven parent POM.
- Externalize all configurable values in `application.properties` or `application.yml`.
- Use `@ConfigurationProperties` for grouped configuration binding.

### REST API Development (`spring-boot-starter-web`)
- Annotate controllers with `@RestController` and `@RequestMapping("/api/...")`.
- Use `@Valid` + `@Validated` for request body validation with Jakarta Bean Validation annotations.
- Return `ResponseEntity<T>` for fine-grained HTTP status control.
- Document all endpoints with SpringDoc annotations: `@Operation`, `@ApiResponse`, `@Schema`.

Example:
```java
@RestController
@RequestMapping("/api/admin/cities")
public class CityController {

    @PostMapping
    @Operation(summary = "Create a new city", tags = "Admin - Cities")
    @ApiResponse(responseCode = "201", description = "City created successfully")
    public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CityRequest request) {
        // ...
    }
}
```

### Data Access (`spring-boot-starter-data-jpa`)
- Define entities with `@Entity`, `@Table`, `@Id`, `@GeneratedValue`.
- Use Lombok `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` on entities.
- Create repositories extending `JpaRepository<Entity, IdType>`.
- Use `@Lock(PESSIMISTIC_WRITE)` for concurrency-sensitive operations.
- Use `@Query` for custom JPQL queries; derive queries from method names where possible.

### Security (`spring-boot-starter-security`)
- Configure `SecurityFilterChain` bean with endpoint-specific rules.
- Use role-based access with `hasRole("ADMIN")` / `hasRole("CUSTOMER")`.
- Use in-memory `UserDetailsService` for development (no OAuth/SSO in scope).
- CSRF can be disabled for REST APIs.

### Validation (`spring-boot-starter-validation`)
- Use Jakarta annotations: `@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max`, `@Email`.
- Create custom validators by implementing `ConstraintValidator` for domain-specific rules.
- Group validation constraints where different validation rules apply per operation (create vs update).

### Testing
- **Unit Tests**: JUnit 5 + Mockito for service-layer logic. Mock all dependencies.
- **Integration Tests**: `@SpringBootTest` + `@AutoConfigureMockMvc` for controller testing.
- **Repository Tests**: `@DataJpaTest` for repository layer with embedded database.
- **Security Tests**: `@WithMockUser` for testing role-based access on endpoints.
- **Concurrency Tests**: Use `ExecutorService` + `CountDownLatch` to simulate concurrent booking scenarios.

---

## Maven Skills

### Build Configuration
- Define properties: `<java.version>17</java.version>`
- Use `spring-boot-maven-plugin` for executable JAR packaging.
- Exclude Lombok from the final artifact via plugin configuration.

### Dependency Management
- Let Spring Boot manage dependency versions via the parent POM.
- Add SpringDoc OpenAPI for Swagger UI:
  ```xml
  <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>2.3.0</version>
  </dependency>
  ```

---

## Database & Data Modeling Skills

### Entity Design Principles
- Each entity gets: `@Entity`, `@Table(name = "...")`, `@Id`, `@GeneratedValue(strategy = IDENTITY)`.
- Use `@Enumerated(EnumType.STRING)` for enum fields.
- Define bidirectional relationships explicitly with `mappedBy`.
- Use `@CreationTimestamp` / `@UpdateTimestamp` for audit timestamps.

### Concurrency Strategy
- Use `@Version` (optimistic locking) for entities with low contention.
- Use `@Lock(PESSIMISTIC_WRITE)` on repository methods for seat allocation to prevent double-booking.
- Keep hold durations short and auto-release expired holds via scheduled tasks (`@Scheduled`).

### Indexing
- Add `@Index` annotations on frequently queried columns (e.g., `show_id`, `user_id`, `status`).

---

## API Design Skills

### RESTful Principles
- Use nouns for resources: `/cities`, `/theaters`, `/shows`, `/bookings`.
- Use HTTP methods semantically: POST (create), GET (read), PUT (update), DELETE (delete).
- Use query parameters for filtering, sorting, and pagination.
- Return appropriate HTTP status codes: 201 (created), 200 (success), 400 (bad request), 404 (not found), 409 (conflict).

### Error Response Format
```json
{
    "status": 400,
    "error": "Bad Request",
    "message": "City name must not be blank",
    "path": "/api/admin/cities",
    "timestamp": "2026-07-16T12:00:00Z"
}
```

### Pagination
- Accept `page` (0-indexed) and `size` query parameters.
- Return Spring's `Page<T>` wrapped in a consistent response envelope.

---

## Development Practices

### Git Workflow
- Feature branches off `main`.
- No direct commits or pushes to `main` (enforced by Git hooks in `.githooks/`).
- Descriptive commit messages with conventional commit prefixes.
- Pull requests for all changes.

### Code Quality
- Run `mvn compile` before committing to ensure compilation.
- Run `mvn test` to verify all tests pass.
- Keep methods small and focused (single responsibility).
- Write tests for all public service methods and controller endpoints.
- Maintain at least 80% test coverage on service and controller layers.

---

*Last updated: July 16, 2026*
