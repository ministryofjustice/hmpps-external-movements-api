# HMPPS External Movements API

[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/hmpps-external-movements-api/badge?style=flat)](https://github-community.service.justice.gov.uk/repository-standards/hmpps-external-movements-api)
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-external-movements-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://external-movements-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)
[![Event docs](https://img.shields.io/badge/Event_docs-view-85EA2D.svg)](https://studio.asyncapi.com/?url=https://raw.githubusercontent.com/ministryofjustice/hmpps-external-movements-api/main/async-api.yml&readOnly)

Database Schema diagram: https://ministryofjustice.github.io/hmpps-external-movements-api/schema-spy-report/

A Spring Boot API to manage and track external prisoner movements for the Digital Prison Services. Backend services for https://github.com/ministryofjustice/hmpps-external-movements-ui

## Development
Ensure you have Docker Desktop installed and running.

### Build

```bash
./gradlew clean build
```

### Running Tests

```bash
./gradlew test
```

### Code Quality
```bash
# Run linting and check for code style errors
./gradlew ktLintCheck

# Attempt to fix any style errors automatically
./gradlew ktlintFormat
```
While `ktlintFormat` will attempt to fix any style errors, it may not be able to do so in all cases. Some things to be mindful of include:
1. Wildcard imports - these should be avoided in favour of explicit imports.
2. Line length - this should be kept to 120 characters.

## Common Kotlin patterns

Many patterns have evolved for HMPPS Kotlin applications. Using these patterns provides consistency across our suite of 
Kotlin microservices and allows you to concentrate on building your business needs rather than reinventing the 
technical approach.

Documentation for these patterns can be found in the [HMPPS tech docs](https://tech-docs.hmpps.service.justice.gov.uk/common-kotlin-patterns/). 
If this documentation is incorrect or needs improving please report to [#ask-prisons-digital-sre](https://moj.enterprise.slack.com/archives/C06MWP0UKDE)
or [raise a PR](https://github.com/ministryofjustice/hmpps-tech-docs). 

## Running the application locally

The application comes with a `dev` spring profile that includes default settings for running locally. This is not
necessary when deploying to kubernetes as these values are included in the helm configuration templates -
e.g. `values-dev.yaml`.

There is also a `docker-compose.yml` that can be used to run a local instance of the template in docker and also an
instance of HMPPS Auth (required if your service calls out to other services using a token).

```bash
docker compose pull && docker compose up
```

will build the application and run it and HMPPS Auth within a local docker instance.

### Running the application in Intellij

```bash
docker compose pull && docker compose up --scale hmpps-external-movements-api=0
```

will just start a docker instance of HMPPS Auth. The application should then be started with a `dev` active profile
in Intellij.
