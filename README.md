# Tea Rule Studio

A backend application for managing tea rules and performing simulations.

## Overview

This project is a Ktor-based backend that provides REST APIs for managing rules, tea lots, and running simulations.

## Technologies

- Kotlin
- Ktor (web framework)
- Exposed (ORM for database)
- H2 (in-memory database for testing)
- PostgreSQL (for production)
- kotlinx.serialization (for JSON)

## Getting Started

### Prerequisites

- JDK 17 or later
- Gradle

### Running the Application

```bash
./gradlew run
```

The server will start on http://localhost:8080

### Running Tests

```bash
./gradlew test
```

## API Documentation

### Health Check

- GET /health: Returns database connection status and counts

### Rules

- GET /rules: Get all rules
- POST /rules: Create a new rule
- GET /rules/{id}: Get rule by ID
- PUT /rules/{id}: Update rule
- DELETE /rules/{id}: Delete rule
- GET /export/rules: Export rules
- POST /import/rules: Import rules

### Tea Lots

- GET /tea-lots: Get all tea lots
- POST /tea-lots: Create a new tea lot
- GET /tea-lots/{id}: Get tea lot by ID
- PUT /tea-lots/{id}: Update tea lot
- DELETE /tea-lots/{id}: Delete tea lot
- GET /export/tea-lots: Export tea lots
- POST /import/tea-lots: Import tea lots

### Simulation

- POST /simulate: Run bulk simulation
- POST /simulate/{teaLotId}: Run simulation for a specific tea lot

## Database

The application uses Exposed ORM with H2 for testing and PostgreSQL for production.
