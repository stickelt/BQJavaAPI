package com.example.bqjavaapi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class BigQueryConfig {

    @Value("${google.project.id}")
    private String projectId;
    
    @Value("${google.credentials.path:#{null}}")
    private String credentialsPath;

    @Bean
    public BigQuery bigQuery() throws IOException {
        BigQueryOptions.Builder builder = BigQueryOptions.newBuilder()
                .setProjectId(projectId);
        
        // Use provided credentials file if specified
        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream(credentialsPath));
            builder.setCredentials(credentials);
        }
        // Otherwise, default credentials will be used
        
        return builder.build().getService();
    }
}
