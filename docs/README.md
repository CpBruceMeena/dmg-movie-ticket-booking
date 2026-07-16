# DMG Movie Ticket Booking - Architecture Documentation

This directory contains architecture diagrams for the DMG Movie Ticket Booking System.

## Diagrams

### Class Diagram
- **File**: [`diagrams/class-diagram.drawio`](diagrams/class-diagram.drawio)
- **Description**: Shows all JPA entities, their fields, and relationships (one-to-many, many-to-many)
- **Entities**: City, Theater, Screen, Seat, Show, PricingTier, RefundPolicy
- **Relationships**:
  - City `1 --*` Theater
  - Theater `1 --*` Screen
  - Screen `1 --*` Show
  - Screen `1 --*` Seat
  - Show `* --*` PricingTier (Many-to-Many via `show_pricing_tiers` join table)

### Sequence Diagrams

| Diagram | File | Description |
|---------|------|-------------|
| **Authentication Flow** | [`diagrams/sequence-auth-flow.drawio`](diagrams/sequence-auth-flow.drawio) | User login with JWT token generation via `POST /api/auth/login` |
| **City-Theater-Screen Setup** | [`diagrams/sequence-city-theater-setup.drawio`](diagrams/sequence-city-theater-setup.drawio) | Admin creates city, then theater with city validation |
| **Show Management Flow** | [`diagrams/sequence-show-management.drawio`](diagrams/sequence-show-management.drawio) | Admin creates a show with screen assignment, pricing tiers, and overlap validation |

## Viewing Diagrams

You can view these diagrams by:

1. **Opening in draw.io** (https://app.diagrams.net/) - Click "Open Existing Diagram" and select the `.drawio` file
2. **VS Code Extension** - Install the "Draw.io Integration" extension by Henning Dieterichs
3. **GitHub** - GitHub supports rendering `.drawio` files natively in the repository view

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Database | PostgreSQL (production), H2 (dev/test) |
| ORM | Spring Data JPA / Hibernate |
| Auth | JWT with Spring Security |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Caching | In-memory (ConcurrentHashMap) |
| Rate Limiting | Sliding Window Algorithm |
