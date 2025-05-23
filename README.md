# BQJavaAPI

A Java Spring Boot application for processing BigQuery data and updating it with information fetched from an external API.

## Overview

This application:

1. Queries BigQuery for records where ASPIN_ID is null or empty
2. Makes REST API calls using RX_DATA_ID from these records 
3. Updates BigQuery records with the ASPIN_ID received from the API
4. Processes records efficiently in batches with parallel API calls

## Project Structure

```
BQJavaAPI/
├── build.gradle.kts - Gradle build file (Kotlin DSL)
├── src/
    ├── main/
        ├── java/com/example/bqjavaapi/
        │   ├── BqJavaApiApplication.java - Main application entry point
        │   ├── Record.java - Data model for BigQuery records
        │   ├── BigQueryService.java - Handles BigQuery operations
        │   ├── ApiClientService.java - Makes REST API calls
        │   ├── AspenIdUpdater.java - Core processing logic with thread pool
        │   └── config/
        │       ├── BigQueryConfig.java - BigQuery client configuration
        │       └── RestClientConfig.java - REST client configuration
        └── resources/
            └── application.properties - Application configuration
```

## Configuration

Update `application.properties` with your specific settings:

```properties
# BigQuery configuration
google.project.id=your-google-project-id
# google.credentials.path=/path/to/your/credentials.json

# API configuration
api.base-url=https://your-api-endpoint.com/api

# Processing configuration
app.batch-size=1000
app.concurrency=10
```

## Usage

1. Configure your BigQuery and API settings
2. Run as a Spring Boot application or build a JAR for deployment
3. Schedule execution as needed (e.g., via Kubernetes CronJob)

## Build

```bash
./gradlew build
```
