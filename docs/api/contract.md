# API Contract

**Status:** Draft for B2/B3 review  
**Base path:** `/api/v1`

## 1. Conventions

- Content type: `application/json`.
- JSON field names use `camelCase`.
- IDs use UUID strings.
- Timestamps use ISO-8601 UTC.
- Enum values use `UPPER_SNAKE_CASE`, except analyzer identifiers, which use the agreed lowercase names.
- Authenticated endpoints require `Authorization: Bearer <token>`, except registration, login, and refresh endpoints.
- Validate requests at the API boundary.
- Never expose database entities directly.
- Omit optional fields when absent; do not return them as `null`.
- Use RFC 7807-style errors with content type `application/problem+json`.
- Resource ownership must be enforced in the service layer. Return `404` when a resource belongs to another user.
- Paginated responses use `items`, `page`, `size`, `totalItems`, and `totalPages`. Defaults: `page=0`, `size=20`; maximum `size=100`.

## 2. Submissions — B2

### 2.1 Create a submission

`POST /api/v1/submissions`

Request:

```json
{
  "title": "Example Java submission",
  "originType": "HUMAN",
  "sourceCode": "class Example { public static void main(String[] args) {} }",
  "taskId": "00000000-0000-0000-0000-000000000001",
  "autoAnalyze": true
}
```

`title` and `taskId` are optional. `autoAnalyze` indicates whether analysis should be initiated automatically.

Success: `201 Created`

```json
{
  "submissionId": "00000000-0000-0000-0000-000000000002",
  "originType": "HUMAN",
  "logicalLoc": 1,
  "runId": "00000000-0000-0000-0000-000000000003"
}
```

`runId` is included when an analysis run is created.

Validation:
- `sourceCode` must not be blank.
- `sourceCode` must not exceed 100,000 characters.
- Client-provided `originType` accepts `HUMAN`, `AI_ASSISTED`, or `AI_GENERATED`. `AI_REMEDIATED` is not accepted from clients.
- Empty, binary, or clearly non-Java input returns `422 Unprocessable Content`.
- Java syntax errors are accepted as submissions; analysis records `PARSE_FAILED` with diagnostics.
- `provenanceVerified` is true only for platform-generated or platform-remediated records. Client-created submissions must not be marked verified.

### 2.2 List submissions

`GET /api/v1/submissions`

Query parameters:
- `originType` — optional filter.
- `taskId` — optional UUID filter.
- `page` — default `0`.
- `size` — default `20`, maximum `100`.

Success: `200 OK`

```json
{
  "items": [
    {
      "submissionId": "00000000-0000-0000-0000-000000000002",
      "title": "Example Java submission",
      "originType": "HUMAN",
      "logicalLoc": 1,
      "latestGrade": "A",
      "createdAt": "2026-10-25T09:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

### 2.3 Get a submission

`GET /api/v1/submissions/{submissionId}`

Success: `200 OK`. Returns the submission details, including `sourceCode` and its metadata.

Errors:
- `404 Not Found` — submission does not exist or is not owned by the requester.

### 2.4 Delete a submission

`DELETE /api/v1/submissions/{submissionId}`

Success: `204 No Content`.

Deletion is a soft delete. Unauthorized or unowned resources return `404 Not Found`.

### 2.5 Get submission lineage

`GET /api/v1/submissions/{submissionId}/lineage`

Success: `200 OK`

```json
[
  {
    "submissionId": "00000000-0000-0000-0000-000000000002",
    "originType": "HUMAN",
    "depth": 0,
    "grade": "A"
  }
]
```

### 2.6 Get history trend

`GET /api/v1/history/trend`

Query parameters:
- `dimension` — requested analysis dimension.
- `from` — start date.
- `to` — end date.

The `from` date must not be later than `to`.

Success: `200 OK`

```json
[
  {
    "date": "2026-10-25",
    "submissionId": "00000000-0000-0000-0000-000000000002",
    "originType": "HUMAN",
    "subScore": 85.0
  }
]
```

## 3. Analysis Runs and Reports — B3

### 3.1 Start analysis

`POST /api/v1/submissions/{submissionId}/analysis-runs`

Request:

```json
{
  "analyzers": ["complexity", "duplication"]
}
```

`analyzers` is optional. When supplied, each identifier must be supported.

Success: `202 Accepted`

```json
{
  "runId": "00000000-0000-0000-0000-000000000003",
  "status": "QUEUED"
}
```

Errors:
- `400 Bad Request` — unknown analyzer or invalid request.
- `404 Not Found` — submission does not exist or is not owned by the requester.
- `409 Conflict` — an analysis run is already in progress for the submission. The standard `application/problem+json` response may include `existingRunId` when the existing run is known.

### 3.2 Get analysis run status

`GET /api/v1/analysis-runs/{runId}`

Success: `200 OK`

```json
{
  "runId": "00000000-0000-0000-0000-000000000003",
  "status": "COMPLETED",
  "overallScore": 85.0,
  "grade": "A",
  "scoringVersion": "1.0",
  "dimensions": [
    {
      "dimension": "COMPLEXITY",
      "weightedDensity": 0.15,
      "subScore": 85.0
    }
  ],
  "startedAt": "2026-10-25T09:00:00Z",
  "finishedAt": "2026-10-25T09:00:03Z"
}
```

The `error` field is optional and is included when relevant to a failed run.

### 3.3 Get findings

`GET /api/v1/analysis-runs/{runId}/findings`

Query parameters:
- `severity` — optional severity filter.
- `dimension` — optional dimension filter.
- `page` — default `0`.
- `size` — default `20`, maximum `100`.

Success: `200 OK`

```json
{
  "items": [
    {
      "findingId": "00000000-0000-0000-0000-000000000004",
      "ruleCode": "COMPLEXITY001",
      "cweId": "CWE-674",
      "severity": "MEDIUM",
      "startLine": 10,
      "endLine": 14,
      "message": "Method complexity exceeds the configured threshold.",
      "snippet": "if (condition) { ... }",
      "confidence": 0.95
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

Invalid enum values return `400 Bad Request`.

### 3.4 Get analysis metrics

`GET /api/v1/analysis-runs/{runId}/metrics`

Query parameter:
- `scopeType` — optional scope filter, such as `METHOD`.

Success: `200 OK`

```json
[
  {
    "metricName": "CYCLOMATIC_COMPLEXITY",
    "scopeRef": "Example.calculate",
    "value": 5.0
  }
]
```

### 3.5 Get full analysis report

`GET /api/v1/analysis-runs/{runId}/report`

Success: `200 OK`.

The report contains:
- Overall score and grade.
- Scoring formula and per-finding contributions.
- Findings and explanations.
- Clone or duplication information.
- Analysis metrics.
- Tool, rule, and scoring versions.

The exact report schema must be represented in `openapi.yaml` and agreed upon before implementation.

Errors:
- `404 Not Found` — run does not exist or is not owned by the requester.
- `409 Conflict` — analysis has not finished.

## 4. Common Error Response

Content type: `application/problem+json`.

Example:

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "The request contains an invalid value.",
  "instance": "/api/v1/submissions",
  "code": "VALIDATION_ERROR"
}
```

Expected status codes:
- `400` — malformed request, invalid enum, or validation failure.
- `401` — missing or invalid authentication.
- `404` — missing or unowned resource.
- `409` — conflicting operation or unavailable report.
- `422` — unsupported or invalid source input.
- `500` — unexpected server error.

## 5. Mock Responses and Integration Tests

Mock responses must match the schemas and examples defined in this contract and `openapi.yaml`.

Integration tests must cover:
- Submission creation, listing, retrieval, and deletion.
- Blank, oversized, and non-Java source validation.
- Java syntax errors recorded as `PARSE_FAILED`.
- Cross-user access returning `404`.
- History filtering and date validation.
- Analysis creation returning `202`.
- Concurrent analysis requests returning one `202` and one `409`.
- Invalid analyzer and severity values.
- Report retrieval before completion returning `409`.
- Completed report schema and pagination behavior.

## 6. Contract Change Process

1. Update this document and `openapi.yaml` together.
2. Review changes with backend and frontend owners.
3. Update mock data and integration tests.
4. Implement backend and frontend behavior against the agreed contract.
5. Verify that mocks, OpenAPI schemas, and tests remain consistent.
