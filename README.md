PathShashtra — Backend

PathShashtra is an AI-powered career guidance and study platform. This repository contains the Java Spring Boot backend, which exposes API endpoints and supports application services, authentication, persistence, and integrations.

Frontend repository: https://github.com/RohitMane2005/pathshashtra-frontend
Backend repository: https://github.com/RohitMane2005/pathshashtra-backend
Frontend deployment: https://pathshashtra-frontend.vercel.app

Overview

The backend is built with Spring Boot 3 and Java 17. It uses Spring Web for HTTP APIs, Spring Data JPA for persistence, Spring Security for security controls, JWT libraries for token-based authentication, and PostgreSQL support for relational data.

Technology Stack

Area

Technology

Language

Java 17

Framework

Spring Boot 3.3.5

Build

Maven

HTTP API

Spring Web

Persistence

Spring Data JPA / Hibernate

Security

Spring Security

Token support

JJWT

OAuth client

Spring Security OAuth2 Client

Database

PostgreSQL

Schema migrations

Flyway

Cache

Spring Cache, Caffeine

Redis

Spring Data Redis

Validation

Spring Boot Validation

Email

Spring Boot Mail

HTML sanitization

OWASP Java HTML Sanitizer

Testing

Spring Boot Test, Spring Security Test

Containerization

Docker

Prerequisites

Install the following before running the application:

JDK 17

Maven 3.8+ (or a compatible version)

Git

PostgreSQL for a PostgreSQL-backed local setup

Redis if Redis-backed features are enabled in your configuration

Verify Java and Maven:

java -version
mvn -version

Getting Started

1. Clone the repository

git clone https://github.com/RohitMane2005/pathshashtra-backend.git
cd pathshashtra-backend

2. Configure the environment

Create a local configuration using the settings expected by the application's application.properties or application.yml.

Configuration typically needs to cover:

Active Spring profile and server port

Database URL, username, and password

JWT signing secret and token settings

OAuth client credentials, if OAuth login is enabled

Redis connection settings, if Redis features are enabled

SMTP/email settings, if email features are enabled

External service credentials used by enabled integrations

Allowed frontend origins for CORS

Use the exact property names referenced by the source code and configuration files. Do not assume that generic environment variable names will be recognized automatically.

Do not commit credentials or production secrets. Configure secrets through your local environment or your deployment provider's secret/environment-variable settings.

3. Configure the database

Create a PostgreSQL database and provide its connection details through the application's configuration. Flyway is included for database migrations; allow the application to run the migrations configured in the project.

Before using a production database, confirm that its backup and migration strategy is in place.

4. Run the application

From the repository root:

mvn spring-boot:run

To compile and package the application:

mvn clean package

The generated JAR is normally placed in target/. Run it with:

java -jar target/backend-1.0.0.jar

If the generated JAR has a different name, use the filename present in target/.

By default, Spring Boot uses port 8080 unless the application's configuration overrides it. The frontend's example API URL is http://localhost:8080/api; confirm the actual controller mappings and API prefix in the source before relying on a specific endpoint.

Running Tests

Run the automated test suite with:

mvn test

API Documentation

The API is implemented in the controller classes under src/. Check those controllers and their request/response DTOs for the current routes, required fields, and response formats.

No API documentation URL is specified in this README. If Swagger/OpenAPI is configured, document the actual configured endpoint here after verifying it in the project.

Frontend Integration

The frontend repository is configured through the REACT_APP_API_URL environment variable.

For local development, the frontend example uses:

REACT_APP_API_URL=http://localhost:8080/api

Ensure that:

The frontend points to the correct backend base URL.

CORS permits the frontend's origin.

Authentication headers and token handling match the backend security configuration.

Production frontend and backend URLs are configured separately from local development.

Docker

A Dockerfile is present in the repository. Build the image from the repository root:

docker build -t pathshashtra-backend .

Run the image using the environment configuration required by the application. For example, provide environment variables with docker run -e KEY=value ... or use a suitable environment file. Do not bake secrets into the image.

The exact runtime arguments, exposed port, and required variables should be checked against the repository's Dockerfile and Spring configuration.

Deployment

The backend has been associated with a Render deployment workflow. When deploying:

Connect the backend repository to the hosting provider.

Use JDK 17 and Maven-compatible build settings.

Configure database connectivity and all required environment variables.

Configure CORS for the deployed frontend origin.

Confirm that the application starts successfully and that the health/status endpoint, if configured, responds as expected.

Review logs for database migration, authentication, and external-service errors.

Do not assume that a deployment is healthy just because the build succeeds; verify the running service and key API flows.

Security Considerations

Store JWT signing keys, OAuth secrets, SMTP credentials, database passwords, and third-party API keys outside source control.

Use HTTPS in production.

Use a strong, unique JWT signing secret and appropriate token expiration settings.

Apply authorization checks on protected endpoints.

Validate request payloads and sanitize untrusted HTML where appropriate.

Restrict CORS to trusted frontend origins.

Avoid logging passwords, tokens, session identifiers, or other sensitive data.

Use least-privilege database and Redis credentials.

Keep dependencies updated and review security advisories.

Contributing

Fork the repository.

Create a branch: git checkout -b feat/your-feature.

Keep changes focused and add or update tests where appropriate.

Run mvn test.

Open a pull request with a clear description and testing notes.

License
