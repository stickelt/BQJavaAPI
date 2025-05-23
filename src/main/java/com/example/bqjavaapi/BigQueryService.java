package com.example.bqjavaapi;

import com.google.cloud.bigquery.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BigQueryService {
    private static final Logger logger = LoggerFactory.getLogger(BigQueryService.class);
    private final BigQuery bigQuery;

    public BigQueryService(BigQuery bigQuery) {
        this.bigQuery = bigQuery;
    }

    /**
     * Fetches records from BigQuery where ASPIN_ID is null or empty
     * @param limit maximum number of records to fetch
     * @return List of Records with UUID and RxDataId
     */
    public List<Record> fetchRecordsNeedingAspnId(int limit) {
        logger.info("Fetching up to {} records with null/empty ASPN_ID", limit);
        
        String query = "SELECT uuid, rx_data_id FROM `your_project.your_dataset.your_table` " +
                       "WHERE (ASPN_ID IS NULL OR ASPN_ID = '') " + 
                       "AND rx_data_id IS NOT NULL " +
                       "LIMIT @limit";

        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query)
                .addNamedParameter("limit", QueryParameterValue.int64(limit))
                .build();

        List<Record> records = new ArrayList<>();
        try {
            TableResult result = bigQuery.query(queryConfig);
            result.iterateAll().forEach(row -> {
                String uuid = row.get("uuid").getStringValue();
                String rxDataId = row.get("rx_data_id").getStringValue();
                records.add(new Record(uuid, rxDataId));
            });
            logger.info("Retrieved {} records from BigQuery", records.size());
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
        
        String query = "UPDATE `your_project.your_dataset.your_table` " +
                       "SET ASPN_ID = @aspnId " +
                       "WHERE uuid = @uuid";

        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query)
                .addNamedParameter("aspnId", QueryParameterValue.string(aspnId))
                .addNamedParameter("uuid", QueryParameterValue.string(uuid))
                .build();

        try {
            JobInfo jobInfo = JobInfo.of(queryConfig);
            Job job = bigQuery.create(jobInfo);
            job = job.waitFor();

            if (job.getStatus().getError() == null) {
                logger.info("Successfully updated record {}", uuid);
                return true;
            } else {
                logger.error("Error updating record {}: {}", uuid, job.getStatus().getError());
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Update job interrupted for record {}", uuid, e);
            return false;
        }
    }
}
