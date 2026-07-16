# Skills.md — Skills Used

> This document catalogs the AI skills leveraged during the development of the DMG Movie Ticket Booking System.

---

## What Are Skills?

Skills are reusable, self-contained instruction sets that enable AI agents to perform specific tasks effectively. Each skill encapsulates domain knowledge, best practices, and behavioral guidelines for a particular area.

---

## Skills Used in This Project

### Core Development Skills

| Skill | Purpose |
|-------|---------|
| **Spring Boot Development** | Building REST APIs, entity modeling, JPA repositories, service layer, security configuration. |
| **REST API Design** | Designing clean, consistent, and well-documented RESTful endpoints following industry standards. |
| **Database Modeling** | Entity-relationship design, JPA annotations, schema generation, query optimization. |
| **Exception Handling** | Consistent error handling with Spring's `@ControllerAdvice` and meaningful HTTP status codes. |
| **Input Validation** | Using Jakarta Bean Validation (`@Valid`, custom validators) for request payload validation. |

### Quality Assurance Skills

| Skill | Purpose |
|-------|---------|
| **Unit Testing (JUnit 5)** | Writing thorough unit tests for services, utilities, and edge cases. |
| **Integration Testing** | Testing controller endpoints, security rules, and database interactions with `@SpringBootTest`. |
| **Code Review** | Systematic review of code changes for correctness, style, and edge cases. |

### AI Workflow Skills

| Skill | Purpose |
|-------|---------|
| **Orchestration** | Coordinating multiple AI agents in parallel, managing dependencies between them. |
| **Context Management** | Maintaining relevant context across conversations, pruning when necessary. |
| **Research & Discovery** | Using web search and documentation reading to find the best tools and patterns. |

---

## Skill Installation & Usage

Community skills can be discovered and installed via:

```bash
npx skills find <query>     # Search for skills
npx skills add <owner/repo> --list           # Preview available skills
npx skills add <owner/repo> --skill <name>   # Install a skill
```

Installed skills are stored in `.agents/skills/` and loaded by name during agent sessions.

---

*Last updated: July 16, 2026*
