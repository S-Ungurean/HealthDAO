package org.dao;

import java.util.Optional;

import org.dao.models.JobDTO;
import org.dao.models.JobRequest;
import org.dao.models.Status;

public interface JobDAO {
    void createJob(JobRequest request);

    Optional<JobDTO> findByJobId(String id);

    void updateResultObjectKeyAndStatusAndModelResults(String jobId, String objectKey, Status status, String modelResults);

    void updateInputObjectKey(String jobId, String objectKey);

    void updateStatus(String jobId, Status status);

    boolean hasFileHash(String fileHash);
}
