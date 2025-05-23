package com.example.bqjavaapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Service
public class AspenIdUpdater {
    private static final Logger logger = LoggerFactory.getLogger(AspenIdUpdater.class);
    
    private final BigQueryService bqService;
    private final ApiClientService apiService;
    private final int batchSize;
    private final int concurrency;

    public AspenIdUpdater(BigQueryService bqService, ApiClientService apiService, 
                         @Value("${app.batch-size:1000}") int batchSize,
                         @Value("${app.concurrency:10}") int concurrency) {
        this.bqService = bqService;
        this.apiService = apiService;
        this.batchSize = batchSize;
        this.concurrency = concurrency;
        logger.info("Initialized AspenIdUpdater with batchSize={}, concurrency={}", batchSize, concurrency);
    }

    /**
     * Runs the batch job to process records
     * - Fetches records from BigQuery where ASPIN_ID is null/empty
     * - Makes API calls in parallel (limited by concurrency)
     * - Updates BigQuery with ASPIN_ID when found
     */
    public void runBatchJob() throws InterruptedException {
        logger.info("Starting batch job");
        
        // Fetch records from BigQuery
        List<Record> records = bqService.fetchRecordsNeedingAspId(batchSize);
        logger.info("Processing {} records", records.size());
        
        if (records.isEmpty()) {
            logger.info("No records to process");
            return;
        }

        // Create thread pool for parallel processing
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        List<Future<?>> futures = new CopyOnWriteArrayList<>();
        
        // Submit each record for processing
        for (Record record : records) {
            futures.add(pool.submit(() -> processRecord(record)));
        }

        // Wait for all tasks to complete
        int completed = 0;
        int successful = 0;
        
        for (Future<?> future : futures) {
            try {
                Boolean result = (Boolean) future.get();
                if (result != null && result) {
                    successful++;
                }
                completed++;
                
                // Log progress for larger batches
                if (completed % 100 == 0) {
                    logger.info("Progress: {}/{} records processed", completed, records.size());
                }
            } catch (ExecutionException ex) {
                logger.error("Error processing record", ex.getCause());
            }
        }
        
        logger.info("Batch job completed. Processed: {}, Successfully updated: {}", completed, successful);
        pool.shutdown();
    }

    /**
     * Processes a single record
     * - Makes API call to fetch ASPIN_ID
     * - Updates BigQuery if ASPIN_ID is found
     * @return true if record was updated successfully
     */
    private Boolean processRecord(Record record) {
        try {
            logger.debug("Processing record: {}", record);
            
            // Call API to get ASPIN_ID
            Optional<String> aspinIdOpt = apiService.fetchAspenId(record.getRxDataId());
            
            // Update BigQuery if ASPIN_ID was found
            return aspinIdOpt.map(aspinId -> bqService.updateAspinId(record.getUuid(), aspinId))
                           .orElse(false);
        } catch (Exception ex) {
            logger.error("Error processing record: {}", record, ex);
            return false;
        }
    }
}
