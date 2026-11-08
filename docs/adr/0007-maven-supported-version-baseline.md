# ADR-0007: Maven build and supported version baseline

**Status:** Accepted  
**Date:** 2026-11-08  
**Deciders:** Principal Engineer, Backend Lead, CTO

## Context

Societix needs one reproducible build path and a version baseline that is still supported upstream.
The documentation previously mixed Gradle and Maven commands and referenced version lines that no
longer represented the latest LTS or supported stable baseline, so generated code, developer setup,
and CI could disagree.

The baseline must favor GA releases, long-lived lines where upstream offers them, and compatible
versions verified by the owning projects rather than by recollection.

## Decision

We will use **Apache Maven**, invoked through the committed Maven Wrapper, as the only backend build
system. Gradle build files and commands are not part of the Societix build contract.

The fixed baseline is:

| Component | Baseline |
|---|---|
| Java | 25 (LTS) |
| Spring Boot | 4.1.2 (GA, open-source-supported line) |
| Spring Modulith | 2.1.2 |
| Apache Maven | 3.10.0 |
| Maven Wrapper | 3.3.4 (`only-script`) |
| Testcontainers for Java | 2.0.5 |
| PostgreSQL / pgvector | PostgreSQL 18 + pgvector 0.8.7 (`pgvector/pgvector:0.8.7-pg18`) |
| Redis | 8.2 Extended Support (`redis:8.2-alpine`) |
| Node.js | 26 (LTS) |

Testcontainers 2.x module names use the `testcontainers-` prefix, including
`testcontainers-postgresql` and `testcontainers-junit-jupiter`. Spring MVC applications use the
Spring Boot 4 `spring-boot-starter-webmvc` starter.

Patch updates within these lines are routine maintenance. A major or minor line change that alters
compatibility or support commitments requires a superseding ADR.

## Compatibility and support evidence

- [Oracle Java SE Support Roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
  identifies Java 25 as the latest LTS release.
- [Spring Boot](https://spring.io/projects/spring-boot/) publishes 4.1.2 as the current GA release;
  its [support policy](https://github.com/spring-projects/spring-boot/wiki/Supported-Versions)
  guarantees at least 12 months for minor lines, and its
  [system requirements](https://docs.spring.io/spring-boot/system-requirements.html) include Java 25.
- [Spring Modulith 2.1.2](https://github.com/spring-projects/spring-modulith/releases/tag/2.1.2)
  upgrades its baseline to Spring Boot 4.1.2.
- [Apache Maven release history](https://maven.apache.org/docs/history) lists 3.10.0 as a maintained
  GA series; [Maven Wrapper releases](https://github.com/apache/maven-wrapper/releases) list 3.3.4
  as latest.
- [Testcontainers for Java](https://java.testcontainers.org/) publishes 2.0.5 and documents the 2.x
  Maven artifact names.
- [PostgreSQL versioning](https://www.postgresql.org/support/versioning/) lists PostgreSQL 18 as
  supported through November 2030; the
  [pgvector image tags](https://hub.docker.com/r/pgvector/pgvector/tags?name=pg18) publish
  `0.8.7-pg18`.
- [Redis version management](https://redis.io/docs/latest/operate/oss_and_stack/install/version-mgmt/)
  designates Redis 8.2 as an Extended release supported through September 2030.
- [Node.js releases](https://nodejs.org/en/about/previous-releases) identifies Node.js 26 as LTS.

## Consequences

**Pros**

- Developers and CI run the checked-in `backend/mvnw` wrapper from `backend/` and use the same
  Maven distribution.
- The runtime, framework, libraries, database, cache, containers, and front-end tooling are on
  supported upstream lines with an explicit compatibility trail.
- Code-generation prompts and onboarding instructions produce the same stack as the actual build.

**Cons / costs**

- Moving across framework and Testcontainers major versions requires starter, test annotation, and
  artifact-name migrations.
- Java 25 and Node.js 26 may require developers to update local toolchains.
- Exact image and dependency versions require periodic maintenance rather than silently drifting.

## Alternatives considered

- **Keep Gradle and Maven examples.** Rejected because two build contracts drift and double the CI
  and onboarding surface.
- **Stay on the older baseline.** Rejected because it leaves new work on stale or EOL references
  despite supported compatible releases being available.
- **Use preview or release-candidate lines.** Rejected because the project needs GA dependencies for
  its production baseline.

---

**Related:** [ADR-0001](../archive/adr/0001-modular-monolith-spring-modulith.md) ·
[`../codegen/01-conventions.md`](../codegen/01-conventions.md) ·
[`../codegen/02-tech-blueprint.md`](../codegen/02-tech-blueprint.md)
