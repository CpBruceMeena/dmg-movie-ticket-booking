# Task.md — Admin APIs Setup

> Implementation plan for the DMG Movie Ticket Booking System — Admin APIs, JWT Auth, Rate Limiting & Postgres

---

## Phase 1: Database & Project Config

### Switch from H2 to PostgreSQL
- Add `postgresql` driver dependency to `pom.xml`
- Update `application.properties` with Postgres connection details:
  - DB: `movie-ticket-booking`
  - User: `postgres`
  - Password: `password`
  - Port: `5432`
- Keep H2 for test profiles
- Set `ddl-auto=update` so JPA auto-creates tables

### Required New Dependencies
| Dependency | Purpose |
|-----------|---------|
| `org.postgresql:postgresql` (runtime) | Postgres JDBC driver |
| `io.jsonwebtoken:jjwt-api:0.12.3` | JWT token creation/validation |
| `io.jsonwebtoken:jjwt-impl:0.12.3` | JWT implementation |
| `io.jsonwebtoken:jjwt-jackson:0.12.3` | JWT JSON serialization |

---

## Phase 2: Enums

| Enum | Values |
|------|--------|
| `SeatType` | `REGULAR`, `PREMIUM`, `VIP` |
| `Role` | `ROLE_ADMIN`, `ROLE_CUSTOMER` |
| `BookingStatus` | `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `REFUNDED` |

---

## Phase 3: JPA Entities

### Entity Relationships

```
City (1) ──── (N) Theater (1) ──── (N) Screen (1) ──── (N) Seat
                                                    (1)
                                                     │
                                                     │
                                              Show (N) ──── PricingTier
```

| Entity | Fields | Key Relationships |
|--------|--------|-------------------|
| **City** | id, name, createdAt, updatedAt | OneToMany → Theater |
| **Theater** | id, name, location, cityId | ManyToOne → City; OneToMany → Screen |
| **Screen** | id, name, theaterId, totalSeats | ManyToOne → Theater; OneToMany → Seat |
| **Seat** | id, screenId, rowLabel, seatNumber, seatType | ManyToOne → Screen |
| **Show** | id, screenId, movieTitle, startTime, endTime, basePrice | ManyToOne → Screen; ManyToMany → PricingTier |
| **PricingTier** | id, name (REGULAR/PREMIUM/VIP/WEEKEND), multiplier, basePrice | ManyToMany → Show |
| **RefundPolicy** | id, name, hoursBeforeShow, refundPercentage | standalone |

All entities include `createdAt` / `updatedAt` via `@CreationTimestamp` / `@UpdateTimestamp`.

---

## Phase 4: DTOs

### Request DTOs (with Jakarta Validation)
- `CityRequest` — name (@NotBlank)
- `TheaterRequest` — name, location, cityId (@NotNull)
- `ScreenRequest` — name, theaterId, totalSeats
- `SeatLayoutRequest` — screenId, rows, seatsPerRow (generates seats)
- `ShowRequest` — screenId, movieTitle, startTime, endTime, basePrice
- `PricingTierRequest` — name, multiplier, basePrice, applicableDays
- `RefundPolicyRequest` — name, hoursBeforeShow, refundPercentage

### Response DTOs
- Mirror the request DTOs but include `id`, `createdAt`, `updatedAt`

---

## Phase 5: Repositories

All extend `JpaRepository<Entity, Long>` — Spring Data JPA auto-implements:

| Repository | Custom Query Methods |
|-----------|---------------------|
| `CityRepository` | findByCityId, findAllByOrderByName |
| `TheaterRepository` | findByCityId |
| `ScreenRepository` | findByTheaterId |
| `SeatRepository` | findByScreenId, findByScreenIdAndSeatType |
| `ShowRepository` | findByScreenId, findByMovieTitleContaining |
| `PricingTierRepository` | findByName |
| `RefundPolicyRepository` | (none needed yet) |

---

## Phase 6: Services

Each admin service follows the same pattern:
1. Request DTO → validate
2. Convert to entity
3. Save via repository
4. Convert to response DTO
5. Return

| Service | Key Methods |
|---------|------------|
| `CityService` | create, getAll, getById, update, delete |
| `TheaterService` | create (with city validation), getTheatersByCityId |
| `ScreenService` | create (with theater validation), getScreensByTheaterId |
| `SeatLayoutService` | configureLayout (bulk create seats), getLayoutByScreen |
| `ShowService` | create, getShowsByScreen, getShowsByTheater |
| `PricingService` | create tier, update tier, getAllTiers |
| `RefundPolicyService` | create policy, getActivePolicy, updatePolicy |

---

## Phase 7: Controllers

All admin controllers are under `/api/admin/` and require `ROLE_ADMIN`.

| Controller | Endpoints |
|-----------|-----------|
| `CityController` | POST `/api/admin/cities`, GET `/api/admin/cities`, GET `/api/admin/cities/{id}`, PUT `/api/admin/cities/{id}`, DELETE `/api/admin/cities/{id}` |
| `TheaterController` | POST `/api/admin/theaters`, GET `/api/admin/theaters?cityId=`, GET `/api/admin/theaters/{id}` |
| `ScreenController` | POST `/api/admin/screens`, GET `/api/admin/screens?theaterId=` |
| `SeatLayoutController` | POST `/api/admin/seats/layout`, GET `/api/admin/seats?screenId=` |
| `ShowController` | POST `/api/admin/shows`, GET `/api/admin/shows?screenId=`, GET `/api/admin/shows/{id}` |
| `PricingController` | POST `/api/admin/pricing-tiers`, GET `/api/admin/pricing-tiers`, PUT `/api/admin/pricing-tiers/{id}` |
| `RefundPolicyController` | POST `/api/admin/refund-policies`, GET `/api/admin/refund-policies`, PUT `/api/admin/refund-policies/{id}` |

---

## Phase 8: JWT Authentication

### Architecture
```
JwtTokenProvider          → Generate & validate tokens, extract roles
JwtAuthenticationFilter   → OncePerRequestFilter: read token from header, validate, set SecurityContext
JwtAuthenticationEntryPoint → Return 401 for unauthenticated requests
SecurityConfig            → SecurityFilterChain: configure endpoint permissions
```

### Token Structure (Claims)
```json
{
  "sub": "admin",
  "roles": ["ROLE_ADMIN"],
  "iat": 1712345678,
  "exp": 1712432078
}
```

### Auth Endpoints
- POST `/api/auth/login` → accepts username/password → returns JWT token (no auth required)
- GET `/api/auth/me` → returns current user info (auth required)

---

## Phase 9: Rate Limiting (Sliding Window)

### Architecture
```
RateLimitingInterceptor   → HandlerInterceptor: intercept all /api/* requests
SlidingWindowCache        → ConcurrentHashMap<String, SlidingWindowEntry>: thread-safe storage
```

### Algorithm
- Each user (identified by JWT subject or IP) has a sliding window of timestamps
- Default: **100 requests per minute** per user
- If window is full → return HTTP 429 Too Many Requests
- Old timestamps (outside 60s window) are evicted on each request

### Configuration
- Configurable via `application.properties`: `rate.limit.max-requests=100`, `rate.limit.window-minutes=1`

---

## Phase 10: Exception Handling

- `GlobalExceptionHandler` with `@ControllerAdvice`
- Covers: validation errors, resource not found, unauthorized, rate limit exceeded, generic 500
- Returns consistent JSON error response

---

## File Structure (New/Modified Files)

```
src/main/java/com/dmg/moviebooking/
├── config/
│   ├── SecurityConfig.java          (NEW)
│   └── RateLimitingConfig.java      (NEW)
├── controller/admin/
│   ├── CityController.java          (NEW)
│   ├── TheaterController.java       (NEW)
│   ├── ScreenController.java        (NEW)
│   ├── SeatLayoutController.java    (NEW)
│   ├── ShowController.java          (NEW)
│   ├── PricingController.java       (NEW)
│   └── RefundPolicyController.java (NEW)
├── dto/request/
│   ├── CityRequest.java             (NEW)
│   ├── TheaterRequest.java          (NEW)
│   ├── LoginRequest.java            (NEW)
│   └── ... (all request DTOs)
├── dto/response/
│   ├── CityResponse.java            (NEW)
│   ├── AuthResponse.java            (NEW)
│   └── ... (all response DTOs)
├── entity/
│   ├── City.java                    (NEW)
│   ├── Theater.java                 (NEW)
│   ├── Screen.java                  (NEW)
│   ├── Seat.java                    (NEW)
│   ├── Show.java                    (NEW)
│   ├── PricingTier.java             (NEW)
│   └── RefundPolicy.java            (NEW)
├── enums/
│   ├── SeatType.java                (NEW)
│   ├── Role.java                    (NEW)
│   └── BookingStatus.java           (NEW)
├── exception/
│   ├── GlobalExceptionHandler.java  (NEW)
│   ├── ResourceNotFoundException.java (NEW)
│   └── RateLimitExceededException.java (NEW)
├── repository/
│   ├── CityRepository.java          (NEW)
│   ├── TheaterRepository.java       (NEW)
│   └── ... (all repositories)
├── security/
│   ├── JwtTokenProvider.java        (NEW)
│   ├── JwtAuthenticationFilter.java (NEW)
│   └── JwtAuthenticationEntryPoint.java (NEW)
├── service/admin/
│   ├── CityService.java             (NEW)
│   ├── TheaterService.java          (NEW)
│   └── ... (all admin services)
├── controller/AuthController.java   (NEW)
├── service/LoginService.java        (NEW)
└── rateLimiting/
    ├── RateLimitingInterceptor.java (NEW)
    └── SlidingWindowCache.java      (NEW)

Modified:
├── pom.xml                          (Add Postgres, JWT deps)
├── src/main/resources/application.properties (Postgres config)
```

---

*Last updated: July 16, 2026*
