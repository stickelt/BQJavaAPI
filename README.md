# BQJavaAPI

A Java Spring Boot application for processing BigQuery data and updating it with information fetched from an external API.

## Overview

This application:

1. Queries BigQuery for records where ASPN_ID is null or empty
2. Makes API calls using RX_DATA_ID from these records (currently mocked)
3. Updates BigQuery records with the ASPN_ID received from the API
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
        │   ├── ApiClientService.java - Makes mocked API calls
        │   ├── AspnIdUpdater.java - Core processing logic with thread pool
        │   └── config/
        │       ├── BigQueryConfig.java - BigQuery client configuration
        │       └── RestClientConfig.java - REST client configuration
        └── resources/
            └── application.yaml - Application configuration (YAML format)
```

## Configuration

Update `application.yaml` with your specific settings:

```yaml
# BigQuery configuration
google:
  project:
    id: your-google-project-id
  # credentials:
  #   path: /path/to/your/credentials.json

# API configuration
api:
  base-url: https://your-api-endpoint.com/api

# Processing configuration
app:
  batch-size: 1000
  concurrency: 10
  
# BigQuery dataset and table
bigquery:
  dataset: your_dataset
  table: your_table
```

## Mock API Implementation

Currently, the application uses a mock implementation of the API service that:

1. Simulates API calls with random latency (50-300ms)
2. Randomly returns an ASPN_ID with ~60% probability
3. Returns empty results ~40% of the time

This allows testing the application workflow without an actual API endpoint. The mock implementation is in `ApiClientService.java` and includes commented-out code for the real implementation.

## Usage

1. Configure your BigQuery settings in `application.yaml`
2. Point it to your existing BigQuery table (with 89,000+ records)
3. Run as a Spring Boot application: `./gradlew bootRun`
4. The application will process records in batches, update them if an ASPN_ID is found

## Build

```bash
./gradlew build
```

## Production Implementation

To convert this to a production implementation:

1. Uncomment and implement the real API call in `ApiClientService.java`
2. Update the BigQuery query to match your production table schema
3. Configure proper authentication for both BigQuery and the API
4. Schedule execution as needed (e.g., via Kubernetes CronJob)
