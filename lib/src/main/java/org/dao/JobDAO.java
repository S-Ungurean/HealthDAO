package org.dao;

import java.util.Optional;

import org.dao.models.JobDTO;
import org.dao.models.JobRequest;

public interface JobDAO {
    void createJob(JobRequest request);

    Optional<JobDTO> findByJobId(String id);
}
