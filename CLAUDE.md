# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Freshly generated Spring Boot skeleton (Spring Initializr). The only source is `ServerApplication` and a context-load test in `com.blinkerleague.server`; `application.yaml` only sets `spring.application.name`. There is no domain code yet, so update this file as architecture emerges.

## Stack

- Spring Boot 3.3.13, Java 17 toolchain, Gradle wrapper 7.6.4 (Groovy DSL), Lombok.
- Dependencies already declared: Spring MVC (`starter-web`), Data JPA + H2, Data Redis, Spring Session on Redis, Spring Security.
- Boot and Gradle are deliberately kept old: the dev machine runs IntelliJ IDEA 2021.3.1, whose Gradle sync breaks on newer Gradle. Do not upgrade Boot (4.x needs Gradle 8.14+) or the wrapper without confirming the IDE is updated.
- Redis-backed sessions are on the classpath, but the app starts without a running Redis (verified with `bootRun`). Redis connections are lazy, so errors appear only once sessions or Redis are actually used.
- Security locks all endpoints by default and logs a generated dev password at startup. H2 is in-memory; the console is off unless `spring.h2.console.enabled=true`.

## Commands

Use the wrapper (`gradlew.bat` on Windows, `./gradlew` in Git Bash):

- Run: `./gradlew bootRun`
- Build: `./gradlew build`
- All tests: `./gradlew test`
- Single test class: `./gradlew test --tests "com.blinkerleague.server.ServerApplicationTests"`
- Single method: `./gradlew test --tests "com.blinkerleague.server.ServerApplicationTests.contextLoads"`

No lint/format plugin is configured.

## Environment gotcha

IntelliJ IDEA 2021.3.1's Gradle sync (via its bundled Android plugin, which injects `AndroidStudioToolingPlugin`) fails with `NoClassDefFoundError: org/gradle/util/VersionNumber` on newer Gradle (seen on 9.8.2). It is unrelated to the Korean characters in the user path. Hence the pinned Gradle 7.6.4 / Boot 3.3.x. The IDE sync was not re-verified after the downgrade; the CLI wrapper build works.
