package com.example.bqjavaapi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class BigQueryConfig {
    private static final Logger logger = LoggerFactory.getLogger(BigQueryConfig.class);

    @Value("${google.project.id}")
    private String projectId;

    @Value("${google.credentials.path:#{null}}")
    private String credentialsPath;

    private final Environment environment;

    public BigQueryConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public BigQuery bigQuery() throws IOException {
        // Check if we're running in local profile
        boolean isLocalProfile = false;
        for (String profile : environment.getActiveProfiles()) {
            if (profile.equals("local")) {
                isLocalProfile = true;
                break;
            }
        }

        BigQueryOptions.Builder builder = BigQueryOptions.newBuilder()
                .setProjectId(projectId);

        try {
            // Use provided credentials file if specified and file exists
            if (credentialsPath != null && !credentialsPath.isEmpty()) {
                try {
                    GoogleCredentials credentials = GoogleCredentials.fromStream(
                            new FileInputStream(credentialsPath));
                    builder.setCredentials(credentials);
                    logger.info("Using BigQuery credentials from: {}", credentialsPath);
                } catch (IOException e) {
                    if (isLocalProfile) {
                        // In local profile, use mock credentials if file not found
                        logger.warn("Credentials file not found: {}. Using mock credentials for local profile.", credentialsPath);
                        String mockCredentials = "{ " +
                            "\"type\": \"service_account\", " +
                            "\"project_id\": \"" + projectId + "\", " +
                            "\"private_key_id\": \"mock-key-id-12345\", " +
                            "\"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7VJTUt9Us8cKj\\nMzEfYyjiWA4R4/M2bS1GB4t7NXp98C3SC6dVMvDuictGeurT8jNbvJZHtCSuYEvu\\nNMoSfm76oqFvAp8Gy0iz5sxjZmSnXyCdPEovGhLa0VzMaQ8s+CLOyS56YyCFGeJZ\\n-----END PRIVATE KEY-----\\n\", " +
                            "\"client_email\": \"mock-sa@" + projectId + ".iam.gserviceaccount.com\", " +
                            "\"client_id\": \"123456789012345678901\", " +
                            "\"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\", " +
                            "\"token_uri\": \"https://oauth2.googleapis.com/token\", " +
                            "\"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\", " +
                            "\"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/mock-sa%40" + projectId + ".iam.gserviceaccount.com\" " +
                        "}";
                        InputStream mockStream = new ByteArrayInputStream(mockCredentials.getBytes(StandardCharsets.UTF_8));
                        GoogleCredentials credentials = GoogleCredentials.fromStream(mockStream);
                        builder.setCredentials(credentials);
                    } else {
                        // Rethrow for non-local profiles
                        throw e;
                    }
                }
            } else if (isLocalProfile) {
                // No credentials path specified but in local profile - use mock credentials
                logger.warn("No credentials path specified. Using mock credentials for local profile.");
                String mockCredentials = "{ " +
                    "\"type\": \"service_account\", " +
                    "\"project_id\": \"" + projectId + "\", " +
                    "\"private_key_id\": \"mock-key-id-12345\", " +
                    "\"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7VJTUt9Us8cKj\\nMzEfYyjiWA4R4/M2bS1GB4t7NXp98C3SC6dVMvDuictGeurT8jNbvJZHtCSuYEvu\\nNMoSfm76oqFvAp8Gy0iz5sxjZmSnXyCdPEovGhLa0VzMaQ8s+CLOyS56YyCFGeJZ\\n-----END PRIVATE KEY-----\\n\", " +
                    "\"client_email\": \"mock-sa@" + projectId + ".iam.gserviceaccount.com\", " +
                    "\"client_id\": \"123456789012345678901\", " +
                    "\"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\", " +
                    "\"token_uri\": \"https://oauth2.googleapis.com/token\", " +
                    "\"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\", " +
                    "\"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/mock-sa%40" + projectId + ".iam.gserviceaccount.com\" " +
                "}";
                InputStream mockStream = new ByteArrayInputStream(mockCredentials.getBytes(StandardCharsets.UTF_8));
                GoogleCredentials credentials = GoogleCredentials.fromStream(mockStream);
                builder.setCredentials(credentials);
            }
            // Otherwise, default credentials will be used

            return builder.build().getService();
        } catch (IOException e) {
            logger.error("Error setting up BigQuery: {}", e.getMessage());
            throw e;
        }
    }
}
