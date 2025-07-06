package org.dao;

import java.util.Optional;

import org.dao.models.JobDTO;

public interface JobDAO {
    void createJob();

    Optional<JobDTO> findByJobId(int id);
}
