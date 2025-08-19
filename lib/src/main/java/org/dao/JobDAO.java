package org.dao;

import java.util.Optional;

import org.dao.models.JobDTO;
import org.dao.models.JobRequest;
import org.dao.models.Status;

public interface JobDAO {
    void createJob(JobRequest request);

    Optional<JobDTO> findByJobId(String id);

    void updateLinkToObjectAndStatus(String jobId, String objectLink, Status status);

    void updateStatus(String jobId, Status status);
}
