# BQJavaAPI: Real API Integration Guide

Last updated: May 23, 2025

This guide provides detailed instructions for configuring BQJavaAPI to work with a real API instead of the mock implementation. It covers all aspects of configuration, including credentials management, environment variables, and code changes.

## Table of Contents

1. [Overview](#overview)
2. [API Configuration Options](#api-configuration-options)
3. [Implementing the Real API Client](#implementing-the-real-api-client)
4. [Credentials Management](#credentials-management)
5. [Environment Variables Setup](#environment-variables-setup)
6. [IntelliJ IDEA Configuration](#intellij-idea-configuration)
7. [Testing Your API Integration](#testing-your-api-integration)
8. [Troubleshooting](#troubleshooting)

## Overview

BQJavaAPI is designed to:
1. Query BigQuery for records with null/empty ASPN_ID
2. Call an external API to fetch ASPN_ID values using RX_DATA_ID
3. Update BigQuery records with the retrieved ASPN_ID values

Currently, the application uses a mock API implementation that simulates API calls. This guide explains how to replace this mock with a real API integration.

## API Configuration Options

### Configuration Files

API settings can be configured in several places:

1. **Base Configuration**: `src/main/resources/application.yaml`
2. **Environment-Specific**: `src/main/resources/application-{env}.yaml` (where env is local, dev, qa, or prod)

### API Configuration Properties

The following properties control API behavior:

```yaml
# API configuration
api:
  base-url: https://your-real-api-endpoint.com/api
  # Add any additional API configuration properties here
  username: ${API_USERNAME:default-username}
  password: ${API_PASSWORD:default-password}
  timeout-seconds: ${API_TIMEOUT:30}
  max-retries: ${API_MAX_RETRIES:3}
```

### Adding Custom API Properties

To add custom API properties:

1. Add them to your YAML configuration files
2. Create corresponding fields in a configuration class
3. Inject them using `@Value` annotations

Example configuration class:

```java
@Configuration
public class ApiConfig {
    @Value("${api.base-url}")
    private String baseUrl;
    
    @Value("${api.username:#{null}}")
    private String username;
    
    @Value("${api.password:#{null}}")
    private String password;
    
    @Value("${api.timeout-seconds:30}")
    private int timeoutSeconds;
    
    @Value("${api.max-retries:3}")
    private int maxRetries;
    
    // Getters for these properties
    // ...
}
```

## Implementing the Real API Client

### Step 1: Modify ApiClientService.java

The `ApiClientService.java` file already contains commented code for the real implementation. To enable it:

1. Open `src/main/java/com/example/bqjavaapi/ApiClientService.java`
2. Uncomment the real implementation code (lines 57-83)
3. Comment out or remove the mock implementation code (lines 30-55)

### Step 2: Add Required Dependencies

Ensure your `build.gradle.kts` includes the necessary dependencies:

```kotlin
dependencies {
    // Existing dependencies
    implementation("org.springframework.boot:spring-boot-starter-web")
    
    // If your API requires additional libraries (e.g., OAuth), add them here
    // implementation("org.springframework.security.oauth:spring-security-oauth2:2.5.2.RELEASE")
}
```

### Step 3: Customize the API Response Handling

Modify the `ApiResponse` class to match your actual API response structure:

```java
public static class ApiResponse {
    private String aspnId;
    // Add other fields from your API response
    
    // Getters and setters
}
```

## Credentials Management

### API Authentication Options

The application supports several authentication methods:

1. **Basic Authentication**: Username/password
2. **API Key**: Token-based authentication
3. **OAuth**: For more complex authentication flows

### Storing API Credentials

**NEVER store credentials in your code or commit them to version control!**

Instead, use one of these approaches:

1. **Environment Variables** (recommended for development)
2. **External Configuration Files** (outside the project directory)
3. **Secrets Management Service** (for production environments)

### Setting Up API Credentials

#### Option 1: Environment Variables

```bash
export API_USERNAME=your-api-username
export API_PASSWORD=your-api-password
```

#### Option 2: External Properties File

Create a file `api-credentials.properties` outside your project:

```properties
api.username=your-api-username
api.password=your-api-password
```

Then load it in your application:

```java
@PropertySource("file:/path/to/api-credentials.properties")
```

## Environment Variables Setup

### Required Environment Variables

| Variable Name | Description | Example |
|---------------|-------------|---------|
| `API_BASE_URL` | Base URL for your API | `https://api.example.com/v1` |
| `API_USERNAME` | API authentication username | `apiuser` |
| `API_PASSWORD` | API authentication password | `password123` |
| `API_TIMEOUT` | API call timeout in seconds | `30` |
| `API_MAX_RETRIES` | Maximum number of retry attempts | `3` |

### Setting Environment Variables

#### Linux/macOS

```bash
export API_BASE_URL=https://api.example.com/v1
export API_USERNAME=apiuser
export API_PASSWORD=password123
```

#### Windows (Command Prompt)

```cmd
set API_BASE_URL=https://api.example.com/v1
set API_USERNAME=apiuser
set API_PASSWORD=password123
```

#### Windows (PowerShell)

```powershell
$env:API_BASE_URL = "https://api.example.com/v1"
$env:API_USERNAME = "apiuser"
$env:API_PASSWORD = "password123"
```

## IntelliJ IDEA Configuration

### Setting Environment Variables in IntelliJ

1. Open your Run/Debug Configuration
2. Select your Spring Boot configuration
3. Go to the "Configuration" tab
4. In the "Environment variables" field, add:
   ```
   API_BASE_URL=https://api.example.com/v1;API_USERNAME=apiuser;API_PASSWORD=password123
   ```

### Creating a Run Configuration

1. Click "Edit Configurations..." from the run menu
2. Click the "+" button and select "Spring Boot"
3. Set the Main class to `com.example.bqjavaapi.BqJavaApiApplication`
4. Set VM options: `-Dspring.profiles.active=local`
5. Add environment variables as described above
6. Click "Apply" and "OK"

### Using .env Files with EnvFile Plugin

For easier environment variable management:

1. Install the "EnvFile" plugin in IntelliJ
2. Create a `.env` file in your project root (add to .gitignore!)
3. Add your variables:
   ```
   API_BASE_URL=https://api.example.com/v1
   API_USERNAME=apiuser
   API_PASSWORD=password123
   ```
4. In your run configuration, enable "EnvFile" and select your .env file

## Testing Your API Integration

### Manual Testing

1. Configure your API credentials as described above
2. Run the application with the appropriate profile:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```
3. Check the logs for API call results

### Creating a Test Profile

Create a dedicated test profile with a controlled environment:

1. Create `application-apitest.yaml` with your test API configuration
2. Run with this profile:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=apitest'
   ```

## Troubleshooting

### Common Issues

1. **Authentication Failures**
   - Check credentials in environment variables
   - Verify API endpoint URL
   - Ensure proper authentication headers

2. **Connection Timeouts**
   - Increase timeout settings
   - Check network connectivity
   - Verify firewall settings

3. **Parsing Errors**
   - Ensure API response matches expected format
   - Check for changes in the API contract
   - Add debug logging for API responses

### Enabling Debug Logging

Add these settings to your application-local.yaml:

```yaml
logging:
  level:
    root: INFO
    com.example.bqjavaapi: DEBUG
    org.springframework.web.client: DEBUG  # Shows detailed REST client logs
```

### Getting Help

If you encounter issues:

1. Check the application logs for detailed error messages
2. Review the API documentation for your endpoint
3. Verify network connectivity to the API endpoint
4. Test the API independently using tools like Postman or curl
