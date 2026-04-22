# AGENTS.md

## Source of truth
This repository must be analyzed with source code and runtime/config files as the primary source of truth.
Documentation files are NOT authoritative. README, design docs, and report drafts may be consulted only as hints for file discovery, never as evidence.

## Evidence rules
Every architectural conclusion must cite concrete code/config evidence:
- pom.xml / build files
- application.properties / application.yml
- Java source under src/main/java
- Docker / Compose files
- gateway routing config
- messaging / security / tracing / monitoring config classes
- filter / interceptor / controller / service / mapper implementations

If documentation conflicts with code, follow the code.

## Reporting rules
When generating reports:
1. First enumerate all microservices/modules actually present in code.
2. Then extract technologies, versions, and where they are used.
3. Then explain how each technology is applied in this system.
4. Mark uncertain conclusions explicitly.
5. Do not speculate beyond code evidence.

## Focus
The report must emphasize:
- service split and responsibilities
- gateway and routing
- service registry/discovery
- inter-service communication
- messaging/eventing
- persistence and data access
- security and JWT flow
- resilience (retry/circuit breaker/isolation)
- observability (logging, tracing, metrics, admin/actuator)
- deployment topology with Docker/Compose
- whether the project truly meets microservice architecture characteristics