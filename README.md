# BQJavaAPI

Last updated: May 23, 2025 at 12:04 PM

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

1. Configure your BigQuery settings in profile-specific YAML files
2. Create a directory for your credentials: `mkdir -p config`
3. Copy your GCP credentials file to the config directory

### Running the Application

The application now has configurable environments using Spring profiles:

#### Run with Local Profile (Default)

```bash
./gradlew bootRun
```

This will use settings from `application-local.yaml` which points to your personal GCP project.

#### Run with Development Profile

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

#### Run with QA Profile

```bash
./gradlew bootRun --args='--spring.profiles.active=qa'
```

#### Run with Production Profile

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

#### Using Environment Variables

You can override any configuration property using environment variables:

```bash
export BQ_PROJECT_ID=your-project-id
export BQ_DATASET=your_dataset
export BQ_TABLE=your_table
./gradlew bootRun
```

### Processing Mode

The application is now configured to:

1. Process exactly 1000 records at a time from BigQuery
2. Make parallel API calls using a thread pool (configurable concurrency)
3. Update records in BigQuery when an ASPN_ID is found
4. Provide detailed benchmarking information in the logs

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
