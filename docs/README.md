# DMG Movie Ticket Booking - Architecture Documentation

This directory contains architecture documentation and diagrams for the DMG Movie Ticket Booking System.

## Diagrams

### Entity Relationship Diagrams

| Diagram | File | Description |
|---------|------|-------------|
| **Hierarchical ER Diagram** | [`diagrams/hierarchical-er-diagram.svg`](diagrams/hierarchical-er-diagram.svg) | Top-to-bottom data flow: City → Theater → Screen → Show → Booking. Shows all FK relationships in a hierarchical layout. **(NEW)** |
| **Flat ER Diagram** | [`diagrams/er-diagram.svg`](diagrams/er-diagram.svg) | All tables, columns, and FK relationships in a grid layout. Updated with movies table and shows.movie_id FK. |

### Application Architecture

| Diagram | File | Description |
|---------|------|-------------|
| **Application Architecture** | [`diagrams/application-architecture.svg`](diagrams/application-architecture.svg) | Layered architecture diagram showing the 4 layers (Controllers → Services → Repositories → Data Store), cross-cutting concerns, and all components. Updated with MovieController, MovieBrowserController, MovieService, MovieRepository. |

### Sequence Diagrams

| Diagram | File | Description |
|---------|------|-------------|
| **Authentication Flow** | [`diagrams/sequence-auth-flow.svg`](diagrams/sequence-auth-flow.svg) | User login with JWT token generation via `POST /api/auth/login` |
| **Booking Flow** | [`diagrams/sequence-booking-flow.svg`](diagrams/sequence-booking-flow.svg) | Complete booking lifecycle: hold → pay → cancel → refund |
| **Show Management Flow** | [`diagrams/sequence-show-management.svg`](diagrams/sequence-show-management.svg) | Admin creates city, theater, screen, seat layout, movie, and then show (updated with movie creation step) |

## Viewing Diagrams

All diagrams are **standalone SVG files** that render natively in GitHub - no plugins required. Simply open the `.svg` file in the repository browser.

You can also:
1. **Open in browser** - Download and open locally in any web browser
2. **Edit in VS Code** - If you need to modify them, the SVGs use plain elements (rect, line, text, etc.) that can be edited in any text editor or SVG editor

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Database | PostgreSQL (production), H2 (dev/test) |
| ORM | Spring Data JPA / Hibernate |
| Auth | JWT with Spring Security |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| DB Migrations | Flyway (PostgreSQL) |
| Caching | In-memory (ConcurrentHashMap) |
| Rate Limiting | Sliding Window Algorithm |
