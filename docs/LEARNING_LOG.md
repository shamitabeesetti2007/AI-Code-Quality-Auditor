# Engineering Learning Log

## B0 — Foundation

- Created the Spring Boot Maven project using Java 21.
- Started PostgreSQL 16 using Docker Compose.
- Connected Spring Boot to the local PostgreSQL database.
- Confirmed the initial Flyway baseline migration was applied.
- Confirmed the Maven test command succeeds.
- Added shared engineering rules and an initial API contract.

## Decisions to Confirm

- The generated project currently uses Spring Boot 4.0.8, while the build playbook specifies Spring Boot 3.x. Confirm the intended version before finalizing the foundation.
- The initial Flyway V1 migration is empty and already applied. Add actual schema through a new migration rather than editing V1.
- Business API schemas must be agreed upon before implementing analysis endpoints.