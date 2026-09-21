# Java Spring Boot Core

A reusable Spring Boot project skeleton for Java applications.

The goal of this project is to provide a common starting point for future Spring Boot projects. It contains reusable technical and application-level functionality that is not tied to a specific business domain.

Future projects can use this project as a base and add their own business logic on top of it.

## Overview

This project is a single Spring Boot application.

The source code is organized into three main areas:

* `core` - common, business-independent functionality
* `integration` - technology integrations
* `proj` - project-specific business logic

There is a defined dependency direction between them:

```text
           ┌─────────────┐
           │    core     │
           └─────────────┘
              ▲       ▲
              │       │
              │       │
     ┌────────┘       └────────┐
     │                         │
┌─────────────┐         ┌─────────────┐
│ integration │         │    proj     │
└─────────────┘         └─────────────┘
```

The dependency rules are:

```text
core         → depends on nothing
integration  → depends on core
proj         → depends on core
```

This keeps the common functionality independent and makes it easier to reuse in future projects.

---

## Project Structure

The repository is divided into two main directories:

```text
java-spring-boot-core/
├── api/
│   └── openapi.yml
│
├── backend/
│   ├── core/
│   ├── integration/
│   └── proj/
│
├── .github/
│   └── workflows/
│
└── README.md
```

The `api` directory contains the OpenAPI contract.

The `backend` directory contains the Spring Boot application. The three directories inside `backend` are logical architectural boundaries within the same application, not separate projects or Maven modules.

* `core` contains reusable, business-independent functionality.
* `integration` contains technology-specific integrations.
* `proj` contains project-specific business logic.

---

## Core Functionality

The `core` area provides common functionality that can be reused across projects:

* **Global Exception Handling** - centralizes exception handling and provides a consistent API error response.
* **Internationalization (i18n)** - provides localized messages with configurable locale handling.
* **Aspect-Oriented Logging** - provides annotation-based method execution logging with configurable log levels, arguments, and results.
* **HTTP Request Logging** - logs incoming HTTP requests with their method, URI, response status, and execution time.

---

# Technology Stack

The project currently uses:

* Java 25
* Spring Boot 4.1.1
* Maven
* Spring Web MVC
* Spring Validation
* Spring AOP / AspectJ
* Lombok
* OpenAPI Generator
* Spotless
* Google Java Format
* JaCoCo
* JUnit / Spring Boot Test

---

# Testing

The project contains both unit tests and integration tests.

The two test types are intentionally separated.

## Unit Tests

Unit tests verify individual pieces of application logic in isolation.

They use the naming convention:

```text
*UnitTests.java
```

They are executed using the Maven `unit-tests` profile.

Example:

```bash
mvn test -Punit-tests
```

## Integration Tests

Integration tests verify that multiple application components work together.

They use the naming convention:

```text
*IntegrationTests.java
```

They are executed using the Maven `integration-tests` profile.

Example:

```bash
mvn test -Pintegration-tests
```

The separation allows unit and integration tests to run independently in CI.

---

# Code Formatting

The project uses Spotless with Google Java Format.

Spotless is responsible for checking the Java source formatting.

The CI pipeline runs Spotless independently from the build and test jobs.

To check formatting locally:

```bash
mvn spotless:check
```

To automatically format the source code:

```bash
mvn spotless:apply
```

The purpose of keeping Spotless as a separate CI job is to make formatting failures independent from compilation and testing.

---

## CI Flow

The pipeline is intentionally split into separate jobs.

```text
                 ┌─────────────┐                ┌──────────────┐
                 │    Build    │                │   Spotless   │
                 └──────┬──────┘                └──────────────┘
                        │
               ┌────────┴────────┐
               │                 │
               ▼                 ▼
       ┌──────────────┐  ┌──────────────────┐
       │ Unit Tests   │  │ Integration Tests│
       └──────┬───────┘  └────────┬─────────┘
              │                   │
              │   JaCoCo data     │
              └─────────┬─────────┘
                        ▼
                ┌──────────────┐
                │   Coverage   │
                └──────────────┘
```

Spotless is independent and can run in parallel with the build.

The build runs once and its output is uploaded as an artifact.

After the build succeeds:

* Unit tests run using the build artifacts.
* Integration tests run using the build artifacts.

This avoids running the build again in the test jobs.

After both test jobs succeed:

* Their JaCoCo artifacts are collected.
* The coverage job merges the unit and integration test coverage data.
* The 90% minimum coverage requirement is checked.
* The final coverage report is generated.
