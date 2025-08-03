package org.dao;

import java.util.Optional;

import org.dao.models.JobDTO;
import org.dao.models.JobRequest;
import org.dao.models.Status;

public interface JobDAO {
    void createJob(JobRequest request) throws Exception;

    Optional<JobDTO> findByJobId(String id) throws Exception;

    void updateLinkToObjectAndStatus(String jobId, String objectLink, Status status) throws Exception;

    void updateStatus(String jobId, Status status) throws Exception;
}
