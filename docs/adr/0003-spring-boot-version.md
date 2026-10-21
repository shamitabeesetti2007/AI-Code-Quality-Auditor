# ADR 0003: Spring Boot Version

- Status: Accepted
- Context: The build playbook specifies Spring Boot 3.x, but Spring Initializr offered Spring Boot 4.0.8 in the available options. The generated project uses Java 21 and currently builds successfully.
- Decision: Retain Spring Boot 4.0.8 for the project.
- Consequences: Dependencies, security configuration, testing libraries, and API documentation integration must be verified for Spring Boot 4 compatibility.
- Follow-up: Run tests after dependency changes and verify Springdoc/OpenAPI compatibility before finalizing API documentation.
