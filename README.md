# 🎬 DMG Movie Ticket Booking System

An **SDE-2 Take-Home Assignment** — A scalable movie ticket booking system built with **Java 17** and **Spring Boot**.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Assumptions & Design Decisions](#assumptions--design-decisions)
- [API Endpoints](#api-endpoints)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Running Tests](#running-tests)
- [Development Workflow](#development-workflow)

---

## Overview

This project implements a **Movie Ticket Booking System** that supports:

- **Multiple cities**, each with multiple **theaters**, each offering multiple **shows**.
- **Seat-level booking** with time-bound holds that auto-release if payment isn't completed.
- **Pricing tiers** (Regular, Premium, VIP) and **discount codes**.
- **Full booking lifecycle**: browse → select → hold → pay → confirm → (cancel → refund).
- **Concurrency-safe** seat allocation to prevent double-booking.
- **Role-Based Access Control**: Admin and Customer roles.
- **Asynchronous notifications** for booking confirmations and reminders.

### Roles

| Role | Capabilities |
|------|-------------|
| **Admin** | Manage cities, theaters, shows, seat layouts, pricing tiers, and refund policies. |
| **Customer** | Browse shows, book/cancel seats, view booking history. |

---

## Tech Stack

| Technology | Purpose |
|-----------|---------|
| **Java 17** | Core language. |
| **Spring Boot 3.2** | Application framework. |
| **Spring Data JPA / Hibernate** | ORM and database access. |
| **Spring Security** | Role-based access control (RBAC). |
| **Spring Validation** | Request payload validation. |
| **PostgreSQL** | Primary database (production). |
| **H2 Database** | In-memory database for development/testing. |
| **Redis** | Distributed seat hold management. |
| **Lombok** | Boilerplate reduction. |
| **SpringDoc OpenAPI** | API documentation (Swagger UI). |
| **JWT (jjwt)** | JSON Web Token generation & validation (RSA-256). |
| **JUnit 5 + Mockito** | Unit and integration testing. |
| **Maven** | Build and dependency management. |

---

## Assumptions & Design Decisions

All meaningful assumptions made during development are documented below.

### Domain Modeling

- **City** → **Theater** → **Screen** → **Seat** forms a strict hierarchy.
- Each **Show** is associated with a specific Screen (not just a Theater), allowing different screens to run different movies simultaneously.
- **Seats** have a **SeatType** (REGULAR, PREMIUM, VIP) that maps to base pricing.
- **Pricing** is calculated dynamically based on seat type, show timing (weekday vs weekend), and optional discount codes.

### Booking & Concurrency

- Seat **holds** expire after a configurable duration (default: 5 minutes). Held seats cannot be booked by other users.
- Seat holds are managed via the **SeatHoldManager** interface (Redis or In-Memory), with atomic operations to prevent double-booking.
- Booking status flow: `PENDING_PAYMENT` → `CONFIRMED` → `CANCELLED` / `REFUNDED`.
- **Optimistic locking** (`@Version`) prevents race conditions during concurrent refund operations.

### Payments & Refunds

- Payment processing is handled inline in `BookingService.processPayment()` (no separate `PaymentService` interface).
- Refund policies are configurable via the `refund_policies` table. Default seeded policy: **100% refund if cancelled more than 24 hours before show**, **50% if 2-24 hours before**, **0% if less than 2 hours**.

### Notifications

- Notifications are **asynchronous** using Spring's `@Async` with a `ThreadPoolTaskExecutor`.
- A stub `LogNotificationService` logs notifications to a dedicated `NOTIFICATION` logger instead of sending real emails/SMS.

### Security

- **JWT Bearer Token** authentication using RSA-256 signed tokens (asymmetric key pair).
- **Role-based access control** (ADMIN / CUSTOMER) enforced via Spring Security URL patterns.
- Users are stored in the database (`users` table), not in-memory.
- Real authentication (OAuth/SSO/MFA) is **out of scope** per the requirements.

### API Design

- All APIs are RESTful with consistent JSON responses.
- Error responses follow a standard format: `{ "status": 400, "error": "Bad Request", "message": "...", "timestamp": "..." }`.
- List endpoints return all results (no pagination).

---

## API Endpoints

### Admin APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/cities` | Create a city |
| GET | `/api/admin/cities` | List all cities |
| POST | `/api/admin/theaters` | Create a theater under a city |
| GET | `/api/admin/theaters?cityId={id}` | List theaters in a city |
| POST | `/api/admin/screens` | Add a screen to a theater |
| POST | `/api/admin/seats` | Configure seat layout for a screen |
| POST | `/api/admin/shows` | Create a show |
| GET | `/api/admin/shows?theaterId={id}` | List shows for a theater |
| POST | `/api/admin/pricing-tiers` | Create a pricing tier |
| GET | `/api/admin/pricing-tiers` | List all pricing tiers |
| PUT | `/api/admin/pricing-tiers/{id}` | Update a pricing tier |
| POST | `/api/admin/discount-codes` | Create a discount code |
| POST | `/api/admin/refund-policies` | Create a refund policy |
| GET | `/api/admin/refund-policies` | List all refund policies |
| PUT | `/api/admin/refund-policies/{id}` | Update a refund policy |
| GET | `/api/admin/cities/{id}` | Get city by ID |
| PUT | `/api/admin/cities/{id}` | Update a city |
| DELETE | `/api/admin/cities/{id}` | Delete a city |
| GET | `/api/admin/theaters/{id}` | Get theater by ID |
| GET | `/api/admin/shows/{id}` | Get show by ID |
| GET | `/api/admin/screens/{id}` | Get screen by ID |
| GET | `/api/admin/screens?theaterId={id}` | List screens in a theater |

### Customer APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/cities` | List available cities |
| GET | `/api/theaters?cityId={id}` | List theaters in a city |
| GET | `/api/shows?theaterId={id}` | List shows at a theater |
| GET | `/api/shows/{showId}/seats` | View available seats for a show |
| POST | `/api/bookings/hold` | Hold seats (start booking) |
| POST | `/api/bookings/{id}/pay` | Complete payment & confirm booking |
| POST | `/api/bookings/{id}/cancel` | Cancel a booking |
| POST | `/api/bookings/{id}/refund` | Refund a confirmed booking |
| GET | `/api/bookings/{id}` | View booking details |
| GET | `/api/bookings` | View authenticated user's booking history |

---

## Getting Started

### Prerequisites

- **Java 17+** installed
- **Maven 3.8+** installed (or use the Maven Wrapper)

### Clone & Run

```bash
# Clone the repository
git clone https://github.com/CpBruceMeena/dmg-movie-ticket-booking.git
cd dmg-movie-ticket-booking

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

Access the H2 Console at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:movieticketbooking`).

Access the **Swagger UI** at `http://localhost:8080/swagger-ui/index.html`.

Access the **OpenAPI JSON spec** at `http://localhost:8080/v3/api-docs`.

### Enable Branch Protection Hooks

This repository includes **Git hooks** that prevent direct commits and pushes to the `main` branch. All changes must go through feature branches and pull requests.

After cloning, enable the hooks:

```bash
git config core.hooksPath .githooks   # Enable branch protection hooks
```

### Default Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Customer | `customer` | `customer123` |

---

## Documentation

Comprehensive technical documentation is available:

- **[Technical Architecture](docs/technical-architecture.md)** — Complete architecture overview, database design, business decisions, application flows, and sequence diagrams.
- **Architecture Diagrams (SVG)** — Located in `docs/diagrams/`:
  - [Entity Relationship Diagram](docs/diagrams/er-diagram.svg) — All database tables, columns, and FK relationships
  - [Application Architecture](docs/diagrams/application-architecture.svg) — Layered architecture with cross-cutting concerns
  - [Auth Flow Sequence](docs/diagrams/sequence-auth-flow.svg) — Registration, login, and JWT token authentication
  - [Booking Flow Sequence](docs/diagrams/sequence-booking-flow.svg) — Complete booking lifecycle
  - [Show Management Sequence](docs/diagrams/sequence-show-management.svg) — City/theater/screen/show setup flow

---

## Project Structure

```
src/
├── main/
│   ├── java/com/dmg/moviebooking/
│   │   ├── MovieBookingApplication.java
│   │   ├── config/              # Security, Async, Cache, Rate Limiting, Redis, OpenAPI
│   │   ├── controller/          # REST controllers
│   │   ├── dto/                 # Request/Response DTOs
│   │   ├── entity/              # JPA entities
│   │   ├── enums/               # Enumerations
│   │   ├── exception/           # Custom exceptions & handlers
│   │   ├── ratelimiting/        # Rate limiting implementation
│   │   ├── repository/          # JPA repositories
│   │   ├── security/            # JWT auth filter, provider, entry point
│   │   └── service/             # Business logic
│   └── resources/
│       ├── application.properties
│       ├── application-h2.properties
│       └── test/
└── test/
    └── java/com/dmg/moviebooking/  # Unit & integration tests
```

---

## Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=BookingServiceTest

# Run tests with coverage report
mvn verify
```

---

## Development Workflow

- **[Agents.md](agents.md)** — Development agents, workflow phases, conventions, and code standards.
- **[Skills.md](skills.md)** — Technical skills required: Java 17, Spring Boot 3.2, Maven, JPA, Security, testing, and API design.
- **Swagger UI** — Browse and test all REST APIs at `/swagger-ui/index.html` after starting the application.

---

## Video Walkthrough

A 10-minute Loom video explaining the high-level approach, tech stack reasoning, development workflow, and testing strategy is available:
<!-- Add Loom link here -->

---

## License

This project is created as part of an SDE-2 take-home assessment.

---

*Built with Java + Spring Boot + ❤️*
