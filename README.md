# ARSW Collaborative Architecture Board — Lab 04 Starter

This repository is the starting point for **Lab #4 — Architecture Foundation**.

The goal is **not** to practice REST syntax. The goal is to build a small backend with explicit architectural boundaries, dependency inversion, constructor injection, consistent error handling, tests, and architecture evidence.

## Technology baseline

- Java 21
- Spring Boot 3.x
- Maven
- In-memory persistence for this lab

## Target architecture

```text
REST Controller
      |
      v
Application Service
      |
      v
BoardRepository (port)
      |
      v
InMemoryBoardRepository (adapter)
```

## What is already provided

- Project and package structure.
- Domain types: `Board`, `BoardElement`, `ElementType`.
- Persistence port and in-memory adapter shell.
- Application service shell.
- REST controller shell.
- Central error contract.
- Documentation templates.
- Disabled tests that describe expected behavior.

## What you must complete

Search for `TODO LAB-04` in the repository.

At minimum, complete:

1. `BoardRepository` operations required by the use cases.
2. `InMemoryBoardRepository` behavior.
3. `BoardApplicationService` use cases.
4. REST request validation and controller behavior.
5. Consistent error mapping.
6. Unit and HTTP-facing tests.
7. `docs/api-contract.md`.
8. Architecture diagrams in `docs/architecture/`.
9. `docs/ADR-001-repository-boundary.md`.
10. `docs/AI_USAGE.md`.

## Run

```bash
mvn spring-boot:run
```

The starter includes a small landing page at:

```text
http://localhost:8080/
```

## Verify

```bash
mvn test
```

The included specification tests are disabled initially. Enable them progressively as you implement the required behavior.

## Continuity rule

Your completed Lab 04 repository becomes the conceptual baseline for **Lab 05 — Interactive Board**. Avoid unnecessary changes to contracts and package boundaries.
