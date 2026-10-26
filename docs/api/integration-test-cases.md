# B2/B3 API Integration Test Cases

## 1. Submissions API — B2

| ID | Test case | Expected result |
|---|---|---|
| B2-01 | Create a submission with valid Java source | `201 Created` |
| B2-02 | Create a submission with blank source code | `400 Bad Request` |
| B2-03 | Create a submission with unsupported source language | `422 Unprocessable Entity` |
| B2-04 | Retrieve an existing submission | `200 OK` |
| B2-05 | Retrieve a non-existent submission | `404 Not Found` |
| B2-06 | List submissions with pagination | `200 OK` with pagination metadata |
| B2-07 | Delete an existing submission | `204 No Content` |
| B2-08 | Create a submission without authentication | `401 Unauthorized` |

## 2. Analysis API — B3

| ID | Test case | Expected result |
|---|---|---|
| B3-01 | Start analysis for an existing submission | `202 Accepted` |
| B3-02 | Start analysis for a missing submission | `404 Not Found` |
| B3-03 | Start another analysis while one is running | `409 Conflict` |
| B3-04 | Retrieve analysis run status | `200 OK` |
| B3-05 | Retrieve findings for a completed run | `200 OK` with findings |
| B3-06 | Retrieve metrics for a completed run | `200 OK` with metrics |
| B3-07 | Retrieve the completed analysis report | `200 OK` |
| B3-08 | Retrieve the report before analysis finishes | `409 Conflict` |
| B3-09 | Retrieve an unknown analysis run | `404 Not Found` |

## 3. History API

| ID | Test case | Expected result |
|---|---|---|
| H-01 | Retrieve submission history | `200 OK` |
| H-02 | Filter history by origin type | `200 OK` with matching records |
| H-03 | Request history with pagination | `200 OK` with pagination metadata |
| H-04 | Request an invalid date range | `400 Bad Request` |

## 4. General API Checks

- Verify response content types are JSON where applicable.
- Verify error responses follow the agreed API error schema.
- Verify users cannot access submissions belonging to other users.
- Verify timestamps use UTC ISO 8601 format.
- Verify pagination respects the documented maximum page size.

## Execution Status

**Status: Planned — not yet executed against a running backend.**

These test cases are based on the proposed API contract. Confirm the endpoint paths, authentication rules, and response schemas with the team before treating them as final acceptance criteria.
## 5. Additional B2/B3 Integration Test Cases

| ID | Test case | Expected result |
|---|---|---|
| B2-09 | Submit source code exceeding 100,000 characters | 400 Bad Request |
| B2-10 | Submit Java code with syntax errors | Submission accepted; analysis status becomes PARSE_FAILED |
| B2-11 | Access another user's submission | 404 Not Found |
| B2-12 | Request history with `from` later than `to` | 400 Bad Request |
| B3-10 | Start analysis with an unsupported analyzer | 400 Bad Request |
| B3-11 | Start concurrent analysis requests for the same submission | One request accepted; the other returns 409 Conflict |
| B3-12 | Start analysis with an unsupported severity filter when retrieving findings | 400 Bad Request |
| B3-13 | Retrieve a report before analysis completes | 409 Conflict |
| B3-14 | Retrieve a completed report | 200 OK with the agreed report schema |
| B3-15 | Request findings with pagination | 200 OK with correct pagination metadata |

**Execution status:** These are planned test cases. They must be executed against the backend when the required services are available.
