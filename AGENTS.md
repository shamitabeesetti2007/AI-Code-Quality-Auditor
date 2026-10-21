# AI Code Quality Auditor — Engineering Rules

## Before working
- Read this file and the relevant docs/specs/<feature>/ files.
- Check docs/api/contract.md before changing API behavior.
- For changes affecting more than three files, write a short plan first.

## Architecture
- Backend: Java 21, Spring Boot, PostgreSQL, Flyway.
- Frontend: React, TypeScript, Vite, Tailwind CSS.
- Keep deterministic code analysis independent of AI services.
- AI output is untrusted input: validate it before using or displaying it.
- Never execute submitted Java code directly on the API host.
- Do not add vector search to the MVP without an approved architecture decision.

## Backend conventions
- Use constructor injection.
- Keep controllers thin; place business logic in services.
- Do not expose persistence entities directly through API responses.
- Validate incoming request DTOs.
- Use consistent error responses and appropriate HTTP status codes.
- Add tests for new business logic and important failure paths.
- Create new Flyway migrations for schema changes. Never edit a migration already applied to a database.

## API-first workflow
1. Define requirements and acceptance criteria.
2. Update docs/api/contract.md and docs/api/openapi.yaml.
3. Update or create a mock response for frontend work.
4. Implement and test the backend.
5. Verify the API using tests and the API console.
6. Wire the frontend to the agreed contract.
7. Update documentation and the learning log.

## Security
- Never commit real credentials, API keys, or tokens.
- Keep secrets in environment variables or local ignored files.
- Do not log submitted source code or sensitive values unnecessarily.
- Treat source code and AI-generated findings as untrusted data.

## Git
- Use task-specific branches.
- Keep commits focused and use descriptive messages.
- Run relevant tests before opening a pull request.
- Do not commit generated build output, IDE files, or local environment files.

## Learning
- Explain unfamiliar architectural choices.
- Record important discoveries and decisions in docs/LEARNING_LOG.md.
- Prefer understandable, maintainable code over clever shortcuts.
