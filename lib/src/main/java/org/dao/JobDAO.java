package org.dao;

import java.util.Optional;

import org.dao.models.JobDTO;
import org.dao.models.JobRequest;

public interface JobDAO {
    void createJob(JobRequest request) throws Exception;

    Optional<JobDTO> findByJobId(String id) throws Exception;

    void updateLinkToObject(String jobId, String objectLink) throws Exception;
}
