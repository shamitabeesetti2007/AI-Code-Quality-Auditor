# Engineering Learning Log

## B0 — Foundation

- Created the Spring Boot Maven project using Java 21.
- Started PostgreSQL 16 using Docker Compose.
- Connected Spring Boot to the local PostgreSQL database.
- Confirmed the initial Flyway baseline migration was applied.
- Confirmed the Maven test command succeeds.
- Added shared engineering rules and an initial API contract.

## Decisions Recorded

- Spring Boot 4.0.8 is retained as the project version. Verify compatibility when adding dependencies and features.
- Flyway V1 is an empty baseline migration that has already been applied. Add the actual schema in a new migration; do not edit V1.
- Business API request and response schemas must be agreed upon before implementing analysis endpoints.

## Deterministic Analysis Engine

- The Java analysis engine is independent of persistence and API contracts. Its local `PARSE_FAILED` status maps by name to the canonical run status when that model is introduced.
- JavaParser parses submitted Java 21 source without execution; any parser diagnostic prevents analyzers from running.
- Complexity conventions and default thresholds are documented in `docs/specs/analysis/engine.md`.