package org.dao;

import java.util.Map;
import java.util.Optional;

import org.dao.models.JobDTO;
import org.dao.models.JobRequest;
import org.dao.models.Status;

public interface JobDAO {

    /** Creates a new job record in Cassandra from the given request. */
    void createJob(JobRequest request);

    /** Returns the job record for the given job ID, or empty if not found. */
    Optional<JobDTO> findByJobId(String id);

    /** Updates status and model results after inference completes. */
    void updateStatusAndModelResults(String jobId, Status status, String modelResults);

    /** Updates the result object key after a PDF is uploaded to S3. */
    void updateResultObjectKey(String jobId, String objectKey);

    /** Updates only the status field for the given job. */
    void updateStatus(String jobId, Status status);

    /** Returns true if a job with the given file hash already exists. */
    boolean hasFileHash(String fileHash);

    /** Merges the given key-value pairs into the job's metadata map. */
    void updateMetadata(String jobId, Map<String, String> metadata);

    /** Creates a text-only survey job record with no associated image. */
    void createTextOnlyJob(String jobId, Map<String, String> metadata);
}
