# Java Spring Boot Core

A reusable Spring Boot skeleton for Java applications.

It contains everything that is shared between projects (error handling, logging, request ids, internationalization, health endpoints, CI) so that a new project only has to add its own business logic and technology integrations.

## Getting Started

Prerequisite: JDK 25. Maven does not have to be installed: `./mvnw` (Maven Wrapper) downloads the Maven version fixed in `backend/.mvn/wrapper/maven-wrapper.properties` on its first run and always uses that one, so the local machine, the CI and the Docker build use the same Maven version.

All commands run in the `backend` directory.

Run the application locally (the `local` profile enables readable text logs and DEBUG logging for `com.example`):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Call the demo endpoint:

```bash
curl -i http://localhost:8080/api/v1/demo
```

```text
HTTP/1.1 200
X-Request-Id: 725a5837-8dc1-4d66-93f1-dda6d7101fd1
Content-Type: application/json

{"message":"Core application is running"}
```

Request the demo error in Hungarian:

```bash
curl -H 'Accept-Language: hu' 'http://localhost:8080/api/v1/demo?error=true'
```

```json
{"detail":"A demó hibát kérésre dobtuk","instance":"/api/v1/demo","status":422,"title":"A kérés nem dolgozható fel","requestId":"fb0ed256-1f30-424c-8ff0-e5e9d040960d"}
```

Check the health of the application (management port):

```bash
curl http://localhost:9091/actuator/health
```

Run the same checks as the CI (formatting, all tests, coverage):

```bash
./mvnw clean verify
```

Build and run the Docker image (from the repository root):

```bash
docker build -t java-spring-boot-core .
docker run -p 8080:8080 -p 9091:9091 java-spring-boot-core
```

## Architecture

The project is a single Spring Boot application. Its code is split into three packages under `com.example`:

* `core` - everything that is shared between projects, including the web infrastructure
* `integration` - technology-specific integrations (for example Kafka or Redis), one subpackage per technology
* `project` - the business logic of the concrete project

The dependency rules:

```text
core         → depends on nothing else in the project
integration  → depends on core
project      → depends on core
```

```text
           ┌─────────────┐
           │    core     │
           └─────────────┘
              ▲       ▲
              │       │
     ┌────────┘       └────────┐
     │                         │
┌─────────────┐         ┌─────────────┐
│ integration │         │   project   │
└─────────────┘         └─────────────┘
```

In addition:

* The technologies inside `integration` must not depend on each other. Classes of the same technology may use each other (`integration/redis/a` → `integration/redis/b` is allowed), but different technologies may not (`integration/redis/a` → `integration/kafka/b` is not).
* `core` must not use the code generated from the OpenAPI specification (`com.example.generated`), because it is generated from the API of the concrete project.

These rules are checked by the architecture tests (`ArchTests`), so a violation fails the build.

When `project` needs a technology, an interface goes into `core`, `integration` implements it, and `project` uses only the interface.

## Project Structure

```text
java-spring-boot-core/
├── api/v1/                     OpenAPI specification (the API code is generated from it)
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/
│       │   ├── core/
│       │   │   ├── exception/  error responses (ProblemDetail), ApiException, BusinessException
│       │   │   ├── logging/    @LogExecution, request id, request logging
│       │   │   └── message/    i18n, locale resolution
│       │   ├── integration/    technology integrations (empty in the core)
│       │   └── project/
│       │       ├── controller/v1/
│       │       └── service/
│       └── main/resources/
│           ├── application.yaml
│           ├── application-local.yaml
│           └── messages/       messages.properties, messages_hu.properties
├── .github/                    CI workflows, Dependabot
├── Dockerfile
└── redocly.yaml                OpenAPI lint rules
```

## Core Functionality

### Error Handling

All error responses use the `ProblemDetail` format (RFC 9457, `application/problem+json`):

* `title` is localized from the `error.title.<status>` message key.
* `detail` is a localized, concrete sentence. For 5xx responses it is always empty, so no internal information leaks out.
* `requestId` is the id of the request (see below).

The error types:

| Exception | Status | Usage |
|---|---|---|
| `BusinessException` | 422 | business rule violation; thrown directly with a message key and optional arguments |
| subclass of `ApiException` | own status | other expected errors (for example your own `NotFoundException` with 404) |
| validation and Spring web errors | 4xx | handled automatically |
| any other exception | 500 | logged at ERROR with the stack trace |

```java
throw new BusinessException("order.limit.exceeded", limit);
```

The `detail` is resolved from the message key in the language of the request. The log contains the key and the arguments, not the translated text.

Exceptions thrown in servlet filters are also turned into `ProblemDetail` responses by the `ExceptionHandlingFilter`.

### Request Id

Every response contains an `X-Request-Id` header. If the incoming request has a valid `X-Request-Id` (`[A-Za-z0-9_-]{1,64}`), it is reused, otherwise a new UUID is generated. The id appears in every log line of the request and in the `requestId` field of error responses.

The id comes from the client, so it is not guaranteed to be unique. In production it should only be accepted from a trusted source (for example a gateway).

### Request Logging

Every HTTP request is logged at INFO with its method, URI, response status and duration:

```text
GET /api/v1/demo -> 200 (10 ms)
```

### Method Logging

`@LogExecution` logs the start and the end (or the failure) of a method call. By default it logs at DEBUG without arguments and result:

```java
@LogExecution(level = LogLevel.INFO, logArguments = true, logResult = true)
public Order createOrder(String customerId, @Redacted String cardNumber) { ... }
```

* `@Redacted` hides a parameter value in the log.
* Arguments, results and exception messages are cut to 500 characters, and line breaks are written as `\r` and `\n`.

### Internationalization

Messages are in `messages/messages*.properties`. The language is taken from the `Accept-Language` header, limited to the supported locales. The final fallback is `messages.properties`.

To add a language, add it to `app.locale.supported-locales` and create `messages_<lang>.properties`. The `MessagesUnitTests` checks that every language has the same keys.

### Logging Format

Without a profile (production) the logs are written to the console as structured JSON (ECS), with the request id as a separate field. The `local` profile and the tests write plain text logs.

### Health and Info Endpoints

The actuator runs on a separate management port (9091), so it is not exposed together with the API. Exposed endpoints:

* `/actuator/health`, with `/actuator/health/liveness` and `/actuator/health/readiness` for Kubernetes probes
* `/actuator/info`, with build information (only when built with Maven)
* `/actuator/metrics`

### Virtual Threads

Requests are processed on virtual threads (`spring.threads.virtual.enabled`).

## API

The endpoints are described in the OpenAPI specification (`api/v1/openapi.yml`), not in this README. The API interfaces and DTOs are generated from it during the build (`com.example.generated`), and the controllers in `project` implement them.

The demo endpoint shows this flow: the generated `DemoApi` interface is implemented by `DemoController`, which calls `DemoService`.

## Configuration

* `application.yaml` contains the production settings, `application-local.yaml` only overrides them for local development.
* Own settings are read into records. For example `LocaleProperties` is annotated with `@ConfigurationProperties(prefix = "app.locale")`, so Spring fills it from the `app.locale.*` keys of the YAML at startup, and `@Validated` checks the values.
* What differs between projects is changed in the YAML, not in the core code. For example a project that only needs English sets `app.locale.supported-locales: en`.
* The values are only in `application.yaml`, there is no hidden default in the code. If `app.locale.default-locale` or `app.locale.supported-locales` is missing, the application stops at startup with an error instead of silently running with a default language.

## Conventions

### Logging

* Put `@LogExecution` on the public entry methods of a class.
* Keep the default DEBUG level. Use INFO only where the call should be visible in production.
* Use `logResult = true` at INFO only for small, non-sensitive results.
* Mark sensitive or large parameters with `@Redacted`.
* Never put secrets (passwords, tokens, personal data) into exception messages: they are logged.

### Filters

The filter order is `RequestIdFilter` (`HIGHEST_PRECEDENCE`) → `RequestLoggingFilter` (`+1`) → `ExceptionHandlingFilter` (`+2`). A new filter must run after these (an `@Order` greater than `HIGHEST_PRECEDENCE + 2`, or no `@Order`), otherwise its errors are not turned into error responses.

### API

* Every change of the API starts in the OpenAPI specification (`api/v1`).
* Responses are not wrapped in an envelope: the body is the resource itself, errors are `ProblemDetail`.

## Limitations

`@LogExecution` and `@Redacted` work through Spring AOP proxies, so:

* calls inside the same class (`this.otherMethod()`) are not logged;
* `private` and `final` methods are not logged;
* objects that are not Spring beans are not logged;
* the annotations must be on the method and parameter of the implementing class. On an interface they have no effect, because method and parameter annotations are not inherited.

## Testing

There are three test types, separated by naming convention and Maven profile:

| Type | Naming | Run separately |
|---|---|---|
| Unit tests | `*UnitTests.java` | `./mvnw test -Punit-tests` |
| Integration tests | `*IntegrationTests.java` | `./mvnw test -Pintegration-tests` |
| Architecture tests | `*ArchTests.java` | `./mvnw test -Parchitecture-tests` |

`./mvnw clean verify` runs all of them, merges the coverage data and checks that at least 90% of the instructions are covered (report: `target/site/jacoco`). With `-DskipTests` the coverage check is skipped as well.

## Code Formatting

The Java code is formatted with Spotless (Google Java Format). `./mvnw verify` fails on unformatted code.

```bash
./mvnw spotless:apply
```

## CI

The CI runs on every pull request and on every push to `main`. It is split into separate jobs:

```text
┌─────────────┐   ┌──────────────┐   ┌──────────────┐
│    Build    │   │   Spotless   │   │ OpenAPI Lint │
└──────┬──────┘   └──────────────┘   └──────────────┘
       │
       ├──────────────────┬──────────────────────┐
       ▼                  ▼                      ▼
┌──────────────┐  ┌───────────────────┐  ┌────────────────────┐
│  Unit Tests  │  │ Integration Tests │  │ Architecture Tests │
└──────┬───────┘  └─────────┬─────────┘  └────────────────────┘
       │    JaCoCo data     │
       └─────────┬──────────┘
                 ▼
         ┌──────────────┐
         │   Coverage   │
         └──────────────┘
```

* Spotless and the OpenAPI lint (Redocly) do not need the build and run in parallel with it.
* The build runs once, and the test jobs use its output.
* The coverage job merges the unit and integration test coverage, generates the report and checks the 90% minimum.

Dependabot opens monthly pull requests for Maven and GitHub Actions updates.

## Technology Stack

* Java 25
* Spring Boot 4.1.1 (Web MVC, Validation, AOP, Actuator)
* Maven (Maven Wrapper)
* OpenAPI Generator, Redocly CLI
* Lombok
* JUnit, Spring Boot Test, ArchUnit
* JaCoCo
* Spotless, Google Java Format
* Docker

## License

[MIT](LICENSE)
