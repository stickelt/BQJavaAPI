package com.example.bqjavaapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class ApiClientService {
    private static final Logger logger = LoggerFactory.getLogger(ApiClientService.class);
    private final String apiUrl;
    private final Random random = new Random();

    public ApiClientService(@Value("${api.base-url:http://mock-api.example.com}") String apiUrl) {
        this.apiUrl = apiUrl;
        logger.info("Initialized MOCK ApiClientService with URL: {}. This is a SIMULATION.", apiUrl);
    }

    /**
     * MOCK implementation: Fetches ASPN_ID from the external API using rxDataId
     * This is a simulation that randomly returns an ASPN_ID or empty result
     * 
     * @param rxDataId the RX data ID to query
     * @return Optional containing ASPN_ID if found, empty otherwise
     */
    public Optional<String> fetchAspnId(String rxDataId) {
        if (rxDataId == null || rxDataId.isEmpty()) {
            logger.warn("Attempted to fetch ASPN_ID with null or empty rxDataId");
            return Optional.empty();
        }

        // Simulate API call latency (50-300ms)
        try {
            Thread.sleep(random.nextInt(250) + 50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate API call - randomly return ASPN_ID or empty
        // Log the would-be API URL (helps with debugging and ensures apiUrl is used)
        logger.debug("MOCK API: Would call URL: {}/get-aspn?rxDataId={}", apiUrl, rxDataId);
        
        // Approximately 60% chance of returning an ASPN_ID
        if (random.nextDouble() < 0.6) {
            String aspnId = "ASPN_" + UUID.randomUUID().toString().substring(0, 8);
            logger.info("MOCK API: Successfully retrieved ASPN_ID: {} for rxDataId: {}", aspnId, rxDataId);
            return Optional.of(aspnId);
        } else {
            logger.info("MOCK API: No ASPN_ID found for rxDataId: {}", rxDataId);
            return Optional.empty();
        }

        /* TODO: REAL IMPLEMENTATION (REPLACE MOCK WHEN READY)
        String url = apiUrl + "/get-aspn?rxDataId=" + rxDataId;
        logger.info("Making API call to: {}", url);

        try {
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(url, ApiResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse apiResponse = response.getBody();
                String aspnId = apiResponse.getAspnId();
                
                if (aspnId != null && !aspnId.isEmpty()) {
                    logger.info("Successfully retrieved ASPN_ID for rxDataId: {}", rxDataId);
                    return Optional.of(aspnId);
                } else {
                    logger.info("No ASPN_ID found for rxDataId: {}", rxDataId);
                }
            } else {
                logger.warn("Failed API call for rxDataId: {}, Status: {}", rxDataId, 
                        response.getStatusCode());
            }
        } catch (RestClientException e) {
            logger.error("Error calling API for rxDataId: {}", rxDataId, e);
        }
        
        return Optional.empty();
        */
    }

    // Simple response class for the API (when using real implementation)
    public static class ApiResponse {
        private String aspnId;
        
        public String getAspnId() {
            return aspnId;
        }
        
        public void setAspnId(String aspnId) {
            this.aspnId = aspnId;
        }
    }
}
