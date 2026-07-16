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
- [AI Workflow](#ai-workflow)

---

## Overview

This project implements a **Movie Ticket Booking System** that supports:

- **Multiple cities**, each with multiple **theaters**, each offering multiple **shows**.
- **Seat-level booking** with time-bound holds that auto-release if payment isn't completed.
- **Pricing tiers** (Regular, Premium, Weekend) and **discount codes**.
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
| **H2 Database** | In-memory database for development. |
| **Lombok** | Boilerplate reduction. |
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

- Seat **holds** expire after a configurable duration (default: 10 minutes). Held seats cannot be booked by other users.
- Locking is handled at the **seat-show** level using **pessimistic locking** (`@Lock(PESSIMISTIC_WRITE)`) to prevent race conditions during concurrent booking attempts.
- Booking states: `PENDING_PAYMENT` → `CONFIRMED` → `CANCELLED` / `REFUNDED`.

### Payments & Refunds

- The payment system is **stubbed** — a `PaymentService` interface abstracts payment processing, with an in-memory stub implementation.
- Refund policies are configurable per theater or globally. Default policy: **100% refund if cancelled more than 24 hours before show**, **50% if 2-24 hours before**, **0% if less than 2 hours**.

### Notifications

- Notifications are **asynchronous** using Spring's `@Async` with a `TaskExecutor`.
- A stub `NotificationService` logs notifications instead of sending real emails/SMS.

### Security

- A **basic RBAC** system is implemented using Spring Security with in-memory users (Admin, Customer).
- Real authentication (OAuth/SSO/MFA) is **out of scope** per the requirements.

### API Design

- All APIs are RESTful with consistent JSON responses.
- Error responses follow a standard format: `{ "status": 400, "error": "Bad Request", "message": "...", "timestamp": "..." }`.
- Pagination is supported for list endpoints.

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
| PUT | `/api/admin/pricing/{seatType}` | Update pricing for a seat type |
| POST | `/api/admin/discount-codes` | Create a discount code |
| PUT | `/api/admin/refund-policy` | Update refund policy |

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
| GET | `/api/bookings/{id}` | View booking details |
| GET | `/api/bookings?userId={id}` | View user's booking history |

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

Access the H2 Console at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:moviebooking`).

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

## Project Structure

```
src/
├── main/
│   ├── java/com/dmg/moviebooking/
│   │   ├── MovieBookingApplication.java
│   │   ├── config/              # Security, Async, etc.
│   │   ├── controller/          # REST controllers
│   │   ├── dto/                 # Request/Response DTOs
│   │   ├── entity/              # JPA entities
│   │   ├── enums/               # Enumerations
│   │   ├── exception/           # Custom exceptions & handlers
│   │   ├── repository/          # JPA repositories
│   │   ├── service/             # Business logic
│   │   └── util/                # Utility classes
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/dmg/moviebooking/
        ├── controller/          # Controller integration tests
        ├── service/             # Service unit tests
        └── repository/          # Repository tests
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

## AI Workflow

This project was developed using an **AI-assisted workflow** with multiple specialized agents.

- **[Agents.md](agents.md)** — Detailed documentation of the AI agent workflow, agent roles, and development pipeline.
- **[Skills.md](skills.md)** — Catalog of AI skills used during development.

---

## Video Walkthrough

A 10-minute Loom video explaining the high-level approach, tech stack reasoning, AI workflow, and testing strategy is available:
<!-- Add Loom link here -->

---

## License

This project is created as part of an SDE-2 take-home assessment.

---

*Built with Java + Spring Boot + ❤️*
