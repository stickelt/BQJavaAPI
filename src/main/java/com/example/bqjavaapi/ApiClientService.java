package com.example.bqjavaapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class ApiClientService {
    private static final Logger logger = LoggerFactory.getLogger(ApiClientService.class);
    private final String apiUrl;
    private final boolean useMockApi;
    private final RestTemplate restTemplate;
    private final Random random = new Random();

    public ApiClientService(
            @Value("${api.base-url:http://mock-api.example.com}") String apiUrl,
            @Value("${api.use-mock:true}") boolean useMockApi,
            @Value("${api.username:}") String username,
            @Value("${api.password:}") String password,
            RestTemplateBuilder restTemplateBuilder) {
        
        this.apiUrl = apiUrl;
        this.useMockApi = useMockApi;
        
        // Configure RestTemplate with basic auth if credentials are provided
        if (!username.isEmpty() && !password.isEmpty()) {
            this.restTemplate = restTemplateBuilder
                    .basicAuthentication(username, password)
                    .build();
            logger.info("Initialized ApiClientService with basic authentication");
        } else {
            this.restTemplate = restTemplateBuilder.build();
            logger.info("Initialized ApiClientService without authentication");
        }
        
        if (useMockApi) {
            logger.info("Using MOCK API implementation (URL: {})", apiUrl);
        } else {
            logger.info("Using REAL API implementation (URL: {})", apiUrl);
        }
    }

    /**
     * Fetches ASPN_ID from the external API using rxDataId
     * Uses either mock or real implementation based on configuration
     * 
     * @param rxDataId the RX data ID to query
     * @return Optional containing ASPN_ID if found, empty otherwise
     */
    public Optional<String> fetchAspnId(String rxDataId) {
        if (rxDataId == null || rxDataId.isEmpty()) {
            logger.warn("Attempted to fetch ASPN_ID with null or empty rxDataId");
            return Optional.empty();
        }

        return useMockApi ? 
                fetchAspnIdMock(rxDataId) : 
                fetchAspnIdReal(rxDataId);
    }
    
    /**
     * MOCK implementation: Simulates fetching ASPN_ID from the external API
     */
    private Optional<String> fetchAspnIdMock(String rxDataId) {
        // Simulate API call latency (50-300ms)
        try {
            Thread.sleep(random.nextInt(250) + 50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Log the would-be API URL (helps with debugging)
        logger.debug("MOCK API: Would call URL: {}/{}", apiUrl, rxDataId);
        
        // Generate a mock ASPN_ID (95% chance of success)
        if (random.nextInt(100) < 95) {
            // Create a mock ASPN_ID with a random number (between 100000 and 999999)
            String mockAspnId = "ASPN_" + (random.nextInt(900000) + 100000);
            logger.info("MOCK API: Successfully retrieved ASPN_ID: {} for rxDataId: {}", mockAspnId, rxDataId);
            return Optional.of(mockAspnId);
        }
        
        // If we reach here, no ASPN_ID was generated
        logger.info("MOCK API: No ASPN_ID found for rxDataId: {}", rxDataId);
        return Optional.empty();
    }
    
    /**
     * REAL implementation: Actually calls the external API to fetch ASPN_ID
     */
    private Optional<String> fetchAspnIdReal(String rxDataId) {
        String url = apiUrl + "/" + rxDataId;
        logger.info("Making REAL API call to: {}", url);

        try {
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(url, ApiResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse apiResponse = response.getBody();
                
                // Check if there are any errors returned by the API
                if (apiResponse.getErrors() != null && !apiResponse.getErrors().isEmpty()) {
                    logger.warn("API returned errors for rxDataId: {}: {}", rxDataId, apiResponse.getErrors());
                    return Optional.empty();
                }
                
                // Check if AspnID is present
                if (apiResponse.getAspnID() != null && apiResponse.getAspnID() > 0) {
                    String aspnId = "ASPN_" + apiResponse.getAspnID();
                    logger.info("Successfully retrieved ASPN_ID: {} for rxDataId: {}", aspnId, rxDataId);
                    return Optional.of(aspnId);
                } else {
                    logger.info("No ASPN_ID found for rxDataId: {}", rxDataId);
                }
            } else {
                logger.warn("Failed API call for rxDataId: {}, Status: {}", rxDataId, 
                        response.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            logger.error("API returned error status for rxDataId: {}, Status: {}, Body: {}", 
                    rxDataId, e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            logger.error("Error calling API for rxDataId: {}", rxDataId, e);
        }
        
        return Optional.empty();
    }

    /**
     * Response class for the real API
     */
    public static class ApiResponse {
        private Integer RxDataId;
        private List<String> Errors = new ArrayList<>();
        private String SubmittedDate;
        private String ProcessedData;
        private Long AspnID;
        
        public Integer getRxDataId() {
            return RxDataId;
        }
        
        public void setRxDataId(Integer rxDataId) {
            this.RxDataId = rxDataId;
        }
        
        public List<String> getErrors() {
            return Errors;
        }
        
        public void setErrors(List<String> errors) {
            this.Errors = errors;
        }
        
        public String getSubmittedDate() {
            return SubmittedDate;
        }
        
        public void setSubmittedDate(String submittedDate) {
            this.SubmittedDate = submittedDate;
        }
        
        public String getProcessedData() {
            return ProcessedData;
        }
        
        public void setProcessedData(String processedData) {
            this.ProcessedData = processedData;
        }
        
        public Long getAspnID() {
            return AspnID;
        }
        
        public void setAspnID(Long aspnID) {
            this.AspnID = aspnID;
        }
    }
}
