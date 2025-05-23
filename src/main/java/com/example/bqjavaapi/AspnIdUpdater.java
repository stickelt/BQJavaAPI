package com.example.bqjavaapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Service
public class AspnIdUpdater {
    private static final Logger logger = LoggerFactory.getLogger(AspnIdUpdater.class);
    
    private final BigQueryService bqService;
    private final ApiClientService apiService;
    private final int batchSize; // Kept for future use if needed
    private final int concurrency;

    public AspnIdUpdater(BigQueryService bqService, ApiClientService apiService, 
                         @Value("${app.batch-size:1000}") int batchSize,
                         @Value("${app.concurrency:10}") int concurrency) {
        this.bqService = bqService;
        this.apiService = apiService;
        this.batchSize = batchSize;
        this.concurrency = concurrency;
        logger.info("Initialized AspnIdUpdater with batchSize={}, concurrency={}", batchSize, concurrency);
    }

    /**
     * Runs the batch job to process records
     * - Fetches records from BigQuery where ASPN_ID is null/empty
     * - Makes API calls in parallel (limited by concurrency)
     * - Updates BigQuery with ASPN_ID when found
     */
    public void runBatchJob() throws InterruptedException {
        logger.info("Starting batch job - limited to exactly 1000 records");
        
        // Record the start time of the entire process
        Instant jobStartTime = Instant.now();
        
        // Fetch records from BigQuery - always limit to 1000 as requested
        List<Record> records = bqService.fetchRecordsNeedingAspnId(1000);
        logger.info("Processing {} records", records.size());
        
        if (records.isEmpty()) {
            logger.info("No records to process");
            return;
        }

        // Record the API processing start time
        Instant apiProcessingStartTime = Instant.now();
        
        // Create thread pool for parallel processing
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        List<Future<?>> futures = new CopyOnWriteArrayList<>();
        
        logger.info("Starting API calls with concurrency level: {}", concurrency);
        
        // Submit each record for processing
        for (Record record : records) {
            futures.add(pool.submit(() -> processRecord(record)));
        }

        // Wait for all tasks to complete
        int completed = 0;
        int successful = 0;
        int apiCallsWithAspnId = 0;
        int apiCallsWithoutAspnId = 0;
        
        for (Future<?> future : futures) {
            try {
                ApiResult result = (ApiResult) future.get();
                completed++;
                
                if (result.wasApiCallSuccessful()) {
                    if (result.hasAspnId()) {
                        apiCallsWithAspnId++;
                        if (result.wasUpdateSuccessful()) {
                            successful++;
                        }
                    } else {
                        apiCallsWithoutAspnId++;
                    }
                }
                
                // Log progress for larger batches
                if (completed % 100 == 0) {
                    logger.info("Progress: {}/{} records processed", completed, records.size());
                }
            } catch (ExecutionException ex) {
                logger.error("Error processing record", ex.getCause());
            }
        }
        
        // Calculate total processing time
        Instant jobEndTime = Instant.now();
        Duration apiProcessingDuration = Duration.between(apiProcessingStartTime, jobEndTime);
        Duration totalJobDuration = Duration.between(jobStartTime, jobEndTime);
        
        // Log detailed benchmark information
        logger.info("=== BENCHMARK RESULTS ===");
        logger.info("Total job duration: {} ms", totalJobDuration.toMillis());
        logger.info("API processing duration: {} ms", apiProcessingDuration.toMillis());
        logger.info("Average time per record: {} ms", totalJobDuration.toMillis() / Math.max(1, records.size()));
        logger.info("Records processed: {}", completed);
        logger.info("API calls returning ASPN_ID: {}", apiCallsWithAspnId);
        logger.info("API calls without ASPN_ID: {}", apiCallsWithoutAspnId);
        logger.info("Successfully updated records: {}", successful);
        logger.info("Update success rate: {}%", records.isEmpty() ? 0 : (successful * 100.0 / records.size()));
        logger.info("==========================");
        
        pool.shutdown();
    }

    /**
     * Processes a single record
     * - Makes API call to fetch ASPN_ID
     * - Updates BigQuery if ASPN_ID is found
     * @return true if record was updated successfully
     */
    private ApiResult processRecord(Record record) {
        ApiResult result = new ApiResult();
        try {
            // Record start time for benchmarking
            Instant startTime = Instant.now();
            logger.debug("Processing record: {}", record);
            
            // Call API to get ASPN_ID
            Optional<String> aspnIdOpt = apiService.fetchAspnId(record.getRxDataId());
            result.setApiCallSuccessful(true);
            
            // Record API call duration
            Instant afterApiCall = Instant.now();
            Duration apiCallDuration = Duration.between(startTime, afterApiCall);
            
            // Update BigQuery if ASPN_ID was found
            if (aspnIdOpt.isPresent()) {
                String aspnId = aspnIdOpt.get();
                result.setHasAspnId(true);
                result.setAspnId(aspnId);
                
                boolean updateSuccess = bqService.updateAspnId(record.getUuid(), aspnId);
                result.setUpdateSuccessful(updateSuccess);
                
                // Record total processing duration including update
                Duration totalDuration = Duration.between(startTime, Instant.now());
                logger.debug("Record {} processed in {} ms (API: {} ms, Update: {} ms)", 
                        record.getUuid(), totalDuration.toMillis(), 
                        apiCallDuration.toMillis(), 
                        totalDuration.minus(apiCallDuration).toMillis());
            } else {
                logger.debug("No ASPN_ID found for record {} (API call took {} ms)", 
                        record.getUuid(), apiCallDuration.toMillis());
            }
            
            return result;
        } catch (Exception ex) {
            logger.error("Error processing record: {}", record, ex);
            return result; // Will have default values (all false)
        }
    }
    
    /**
     * Class to track results of API and update operations for a single record
     */
    private static class ApiResult {
        private boolean apiCallSuccessful = false;
        private boolean hasAspnId = false;
        private boolean updateSuccessful = false;
        private String aspnId;
        
        public boolean wasApiCallSuccessful() { return apiCallSuccessful; }
        public void setApiCallSuccessful(boolean value) { this.apiCallSuccessful = value; }
        
        public boolean hasAspnId() { return hasAspnId; }
        public void setHasAspnId(boolean value) { this.hasAspnId = value; }
        
        public boolean wasUpdateSuccessful() { return updateSuccessful; }
        public void setUpdateSuccessful(boolean value) { this.updateSuccessful = value; }
        
        public String getAspnId() { return aspnId; } // Kept for debugging
        public void setAspnId(String aspnId) { this.aspnId = aspnId; }
    }
}
