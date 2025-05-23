package com.example.bqjavaapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class ApiClientService {
    private static final Logger logger = LoggerFactory.getLogger(ApiClientService.class);
    private final RestTemplate restTemplate;
    private final String apiUrl;

    public ApiClientService(RestTemplate restTemplate, @Value("${api.base-url}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
    }

    /**
     * Fetches ASPIN_ID from the external API using rxDataId
     * @param rxDataId the RX data ID to query
     * @return Optional containing ASPIN_ID if found, empty otherwise
     */
    public Optional<String> fetchAspenId(String rxDataId) {
        if (rxDataId == null || rxDataId.isEmpty()) {
            logger.warn("Attempted to fetch ASPIN_ID with null or empty rxDataId");
            return Optional.empty();
        }

        String url = apiUrl + "/get-aspin?rxDataId=" + rxDataId;
        logger.info("Making API call to: {}", url);

        try {
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(url, ApiResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse apiResponse = response.getBody();
                String aspinId = apiResponse.getAspinId();
                
                if (aspinId != null && !aspinId.isEmpty()) {
                    logger.info("Successfully retrieved ASPIN_ID for rxDataId: {}", rxDataId);
                    return Optional.of(aspinId);
                } else {
                    logger.info("No ASPIN_ID found for rxDataId: {}", rxDataId);
                }
            } else {
                logger.warn("Failed API call for rxDataId: {}, Status: {}", rxDataId, 
                        response.getStatusCode());
            }
        } catch (RestClientException e) {
            logger.error("Error calling API for rxDataId: {}", rxDataId, e);
        }
        
        return Optional.empty();
    }

    // Simple response class for the API
    public static class ApiResponse {
        private String aspinId;
        
        public String getAspinId() {
            return aspinId;
        }
        
        public void setAspinId(String aspinId) {
            this.aspinId = aspinId;
        }
    }
}
