# Agents.md — AI Workflow Documentation

> This document logs the AI agents and workflow used during the development of the DMG Movie Ticket Booking System.

---

## Overview

This project was developed using an AI-assisted workflow. An orchestrating AI agent (Buffy) coordinated multiple specialized sub-agents to research, plan, implement, review, and test the codebase. The workflow follows a structured pipeline:

1. **Context Gathering** — Understand requirements, explore the codebase, and research technologies.
2. **Planning** — Break down the problem into structured tasks using a todo list.
3. **Implementation** — Write code with AI assistance, following project conventions.
4. **Review & Testing** — Code review and automated test execution for quality assurance.
5. **Refinement** — Fix issues identified during review/testing.

---

## Agents Used

| Agent | Role |
|-------|------|
| **Buffy** (Orchestrator) | Strategic coding assistant; orchestrates all agents, makes architectural decisions, and writes code. |
| **File Picker** | Searches the codebase to find relevant files by fuzzy matching. |
| **Code Searcher** | Runs ripgrep-based searches across source files to find specific patterns. |
| **Basher** | Executes terminal commands (build, test, lint) and reports output. |
| **Code Reviewer (DeepSeek Flash)** | Reviews code changes and provides critical feedback. |
| **Web Researcher** | Browses the web to find documentation, libraries, and best practices. |
| **Docs Researcher** | Reads technical documentation for frameworks and libraries. |
| **Browser Use** | Automates browser interactions for frontend verification. |

---

## Workflow Pipeline

### Phase 1: Requirements Analysis
- The orchestrator reads the project specification (PDF) to extract functional and non-functional requirements.
- **Output:** Structured understanding of entities, APIs, roles, constraints, and acceptance criteria.

### Phase 2: Context & Research
- File Picker and Code Searcher agents explore the existing codebase.
- Web Researcher and Docs Researcher gather information on relevant technologies, libraries, and patterns.
- **Output:** Comprehensive context for informed decision-making.

### Phase 3: Planning
- The orchestrator uses `write_todos` to create a step-by-step implementation plan.
- Architectural decisions (tech stack, entity modeling, API design) are documented.
- **Output:** Executable plan with ordered, checkable tasks.

### Phase 4: Implementation
- Code is written following project conventions.
- Each change is scoped and minimal — only what's needed for the task.
- Multiple files can be created/edited in parallel where independent.
- **Output:** Working code changes.

### Phase 5: Review & Validation
- Code Reviewer agent inspects all changes for correctness, edge cases, and style issues.
- Basher runs build commands (`mvn compile`, `mvn test`) to validate compilation and tests.
- Issues found are fed back into the implementation phase.
- **Output:** Verified, compilable, well-reviewed code.

---

## Development Principles

- **Convention-Adherent:** All code follows existing project conventions.
- **Minimal Changes:** Only the necessary changes are made — no scope creep.
- **Reuse-First:** Existing helpers, components, and patterns are reused wherever possible.
- **Quality:** Every change is reviewed and tested before being considered complete.

---

## Commit History

The commit history reflects the iterative nature of AI-assisted development, with each phase producing one or more logical commits.

---

*Last updated: July 16, 2026*
