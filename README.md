# TOTP 2fa Backend Server
This is a backend micro-service to facilitate adding TOTP 2fa security in an application.  This service can generate secrets, one-time passwords, and QR Codes.

This service is intended to be used by the main application/services to fulfill its function.  It is the responsibility of the main application or another service to store a user's secret, and to provide a rescue mechanism if the user loses the preshared secret.  There is no security beyond contract enforcement in this service.  Do not use it as a public endpoint.

Rant: The purpose of this project is twofold.  To provide a useful and fast backend 2fa micro-service, and to demonstrate the power and ease of <https://vertx.io>.  I tire of seeing job postings and IDE support for Spring Boot, when vertx can get the job done faster, more efficiently, and completely unopinionated.  Vertx also yields a faster and more efficient micro-service(<https://www.techempower.com/benchmarks/>).

Version 0.5.0

### What it is:
- 16 Character Base32 secure secret generator
- one-time password generator
- one-time password validator
- embedded secret QA Code generator

### What it is not:
- end-to-end 2fa
- client-side token generator
- user secrets manager
- recovery secrets manager

## Requirements
- Java JRE/JDK 1.8+
- Gradle

## Usage
Clone and run *nix:`./gradlew run`, or Windows:`.\gradlew.bat run`

`/resources/totp.yaml` contains the openapi 3.0 specification for the endpoints.

### TODO:
- export and host html openapi spec
- export and host javadoc
- add maven and ant configurations
- Create docker image
- provide sample kubernetes configuration
- Finish this readme with links and more usage information
- more thorough unit tests
- figure out if there are vertx stubs to unit test the Verticle
