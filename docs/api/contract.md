# API Contract

Status: Initial draft — endpoints must be agreed before implementation.

## Conventions
- Base path: /api/v1
- Content type: application/json
- JSON fields: camelCase
- Validate requests at the API boundary.
- Use consistent error responses.
- Never expose database entities directly.

## Initial platform endpoints

### Health
- GET /actuator/health
- Purpose: report application health.
- This is an operational endpoint, not a substitute for business API health checks.

### Code analysis
- Endpoint: TBD
- Request and response schemas: TBD
- Validation rules: TBD
- Error cases: TBD

### Analysis history
- Endpoints and schemas: TBD

### Findings and AI explanations
- Endpoints and schemas: TBD

## Error response
The final schema must be agreed before business endpoints are implemented.

## Contract change process
1. Update this document and openapi.yaml.
2. Review changes with backend and frontend owners.
3. Update mock data and tests.
4. Implement against the agreed contract.
