package com.example.bqjavaapi;

import com.google.cloud.bigquery.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class BigQueryService {
    private static final Logger logger = LoggerFactory.getLogger(BigQueryService.class);
    private final BigQuery bigQuery;
    
    private final String projectId;
    private final String dataset;
    private final String table;
    private final int batchSize;

    public BigQueryService(BigQuery bigQuery, 
                         @Value("${google.project.id}") String projectId,
                         @Value("${bigquery.dataset}") String dataset,
                         @Value("${bigquery.table}") String table,
                         @Value("${app.batch-size:1000}") int batchSize) {
        this.bigQuery = bigQuery;
        this.projectId = projectId;
        this.dataset = dataset;
        this.table = table;
        this.batchSize = batchSize;
        
        logger.info("Initialized BigQueryService with project={}, dataset={}, table={}, batchSize={}",
                projectId, dataset, table, batchSize);
    }

    /**
     * Fetches records from BigQuery where ASPIN_ID is null or empty
     * @param limit maximum number of records to fetch
     * @return List of Records with UUID and RxDataId
     */
    public List<Record> fetchRecordsNeedingAspnId(Integer limit) {
        // Use configured batch size if no limit is provided
        int queryLimit = (limit != null) ? limit : batchSize;
        logger.info("Fetching up to {} records with null/empty ASPN_ID", queryLimit);
        
        // Start timing the query execution
        Instant startTime = Instant.now();
        
        // Use the configured project, dataset and table
        String fullTableName = String.format("`%s.%s.%s`", projectId, dataset, table);
        
        // Build the query using the parameters from configuration
        String query = String.format("SELECT uuid, rx_data_id FROM %s " +
                       "WHERE (aspn_id IS NULL OR aspn_id != 0) " + 
                       "AND rx_data_id IS NOT NULL " +
                       "LIMIT @limit", fullTableName);

        logger.debug("Executing query: {}", query);
        
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query)
                .addNamedParameter("limit", QueryParameterValue.int64(queryLimit))
                .build();

        List<Record> records = new ArrayList<>();
        try {
            TableResult result = bigQuery.query(queryConfig);
            result.iterateAll().forEach(row -> {
                String uuid = row.get("uuid").getStringValue();
                String rxDataId = row.get("rx_data_id").getStringValue();
                records.add(new Record(uuid, rxDataId));
            });
            
            // Calculate and log the query execution time
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            logger.info("Retrieved {} records from BigQuery in {} ms", 
                     records.size(), duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Query execution interrupted", e);
        }

        return records;
    }

    /**
     * Updates a record in BigQuery with the ASPIN_ID
     * @param uuid the unique identifier for the record
     * @param aspnId the ASPN_ID to update
     * @return true if update was successful
     */
    public boolean updateAspnId(String uuid, String aspnId) {
        logger.info("Updating record {} with ASPN_ID: {}", uuid, aspnId);
        
        // Start timing the update operation
        Instant startTime = Instant.now();
        
        // Use the configured project, dataset and table
        String fullTableName = String.format("`%s.%s.%s`", projectId, dataset, table);
        
        String query = String.format("UPDATE %s " +
                       "SET aspn_id = @aspnId " +
                       "WHERE uuid = @uuid", fullTableName);

        logger.debug("Executing update query: {}", query);
        
        // Extract the numeric part from the ASPN_ID string (e.g., "ASPN_123456" -> 123456)
        String numericPart = aspnId.replace("ASPN_", "");
        long aspnIdValue = 0;
        try {
            aspnIdValue = Long.parseLong(numericPart);
            logger.debug("Converting ASPN_ID '{}' to numeric value: {}", aspnId, aspnIdValue);
        } catch (NumberFormatException e) {
            logger.error("Failed to parse ASPN_ID numeric part: '{}'", numericPart, e);
            return false;
        }
        
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query)
                .addNamedParameter("aspnId", QueryParameterValue.int64(aspnIdValue))
                .addNamedParameter("uuid", QueryParameterValue.string(uuid))
                .build();

        try {
            JobInfo jobInfo = JobInfo.of(queryConfig);
            Job job = bigQuery.create(jobInfo);
            job = job.waitFor();

            // Calculate and log the update execution time
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            if (job.getStatus().getError() == null) {
                logger.info("Successfully updated record {} in {} ms", uuid, duration.toMillis());
                return true;
            } else {
                logger.error("Error updating record {}: {} (after {} ms)", uuid, job.getStatus().getError(), duration.toMillis());
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Update job interrupted for record {}", uuid, e);
            return false;
        }
    }
}
