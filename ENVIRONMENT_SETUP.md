# BQJavaAPI Environment Configuration Guide

## Overview

This document provides a detailed explanation of the environment configuration setup for the BQJavaAPI application. The application uses Spring Boot's profile-based configuration system to support different environments (local, dev, QA, prod) without code changes.

## Configuration Files Structure

The configuration is split into multiple files:

1. **`application.yaml`** - Base configuration with common settings and profile activation
2. **`application-local.yaml`** - Your personal laptop configuration 
3. **`application-dev.yaml`** - Development environment configuration
4. **`application-qa.yaml`** - QA environment configuration
5. **`application-prod.yaml`** - Production environment configuration

## How Spring Boot Profiles Work

Spring Boot uses a layered approach to configuration:

1. It first loads the base `application.yaml` file
2. It then overlays settings from the active profile's configuration file (e.g., `application-local.yaml`)
3. Profile-specific settings override the base settings

## Default Profile

The base `application.yaml` sets `local` as the default profile:

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
```

This means the application will use `application-local.yaml` settings by default unless overridden by an environment variable.

## Environment Variables

All configuration files use the `${ENV_VAR:default}` syntax to allow environment variables to override defaults. For example:

```yaml
google:
  project:
    id: ${BQ_PROJECT_ID:your-dev-project-id}
```

This means:
- Use the value of the `BQ_PROJECT_ID` environment variable if it exists
- Otherwise, use `your-dev-project-id` as the default value

## Key Environment Variables

| Variable Name | Description | Example |
|---------------|-------------|--------|
| `SPRING_PROFILES_ACTIVE` | Active environment profile | `dev`, `qa`, `prod` |
| `BQ_PROJECT_ID` | Google Cloud project ID | `your-project-id` |
| `BQ_DATASET` | BigQuery dataset name | `bq_transactions_dev` |
| `BQ_TABLE` | BigQuery table name | `aspn_data_dev` |
| `BQ_CREDENTIALS_PATH` | Path to Google Cloud credentials | `/path/to/credentials.json` |

## Local Environment Configuration

Your `application-local.yaml` uses specific values for your personal development:

```yaml
google:
  project:
    id: jovial-engine-458300-n6
  credentials:
    path: ./config/gcp-credentials.json

bigquery:
  dataset: kafka_bq_transactions
  table: kafka_messages
```

### Setting Up Local Credentials

To set up your local environment:

1. Create a `config` directory in the project root:
   ```
   mkdir -p config
   ```

2. Copy your Google Cloud credentials to this directory:
   ```
   cp /path/to/your/credentials.json config/gcp-credentials.json
   ```

## Environment-Specific Settings

### Processing Configuration

Each environment has different batch size and concurrency settings optimized for that environment:

| Environment | Batch Size | Concurrency |
|-------------|------------|-------------|
| Local | 100 | 5 |
| Dev | 500 | 8 |
| QA | 800 | 10 |
| Prod | 1000 | 20 |

### Logging Configuration

Logging levels are tailored to each environment:

- **Local**: Verbose debugging for Google Cloud and BigQuery
- **Dev**: Debug for application, less verbose for BigQuery
- **QA**: Standard INFO level logging
- **Prod**: Minimal logging (WARN for root, INFO for application)

## Activating Different Profiles

### Command Line

```
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Environment Variable

```
export SPRING_PROFILES_ACTIVE=qa
./gradlew bootRun
```

### System Property

```java
System.setProperty("spring.profiles.active", "prod");
```

## Modifying Configuration

### Adding New Properties

To add a new configuration property:

1. Add it to the base `application.yaml` with a default value
2. Override it in profile-specific files as needed

### Sensitive Information

Never commit sensitive information (credentials, API keys) to Git. Instead:

1. Use environment variables
2. Use credential files outside the project directory
3. Use Spring Vault or other secure storage solutions for production

## BigQuery Client Configuration

The application uses the Google Cloud BigQuery Java SDK for database operations:

```java
@Configuration
public class BigQueryConfig {
    @Bean
    public BigQuery bigQuery(@Value("${google.project.id}") String projectId,
                           @Value("${google.credentials.path:#{null}}") String credentialsPath) {
        // ... setup code
    }
}
```

This Bean reads values from the active profile's configuration.

## Testing Configuration

When writing tests, you can set the active profile programmatically:

```java
@SpringBootTest
@ActiveProfiles("dev")
public class YourServiceTest {
    // Tests will run with dev profile configuration
}
```

## Troubleshooting

### Checking Active Profile

You can verify the active profile by adding this to your main application class:

```java
@Autowired
private Environment env;

@PostConstruct
public void showActiveProfiles() {
    for (String profileName : env.getActiveProfiles()) {
        System.out.println("Currently active profile: " + profileName);
    }
}
```

### Configuration Loading Issues

If you experience configuration loading issues:

1. Check file naming (must be `application-{profile}.yaml`)
2. Verify profile activation (`--spring.profiles.active=profile`)
3. Check environment variable names and values
4. Enable debug logging for Spring configuration: `logging.level.org.springframework.boot.context.config=DEBUG`
