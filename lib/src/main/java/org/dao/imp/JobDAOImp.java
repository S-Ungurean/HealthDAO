package org.dao.imp;

import java.util.Optional;

import org.dao.JobDAO;
import org.dao.models.JobDTO;
import org.dao.models.JobRequest;
import org.dao.models.Status;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import jakarta.inject.Inject;

public class JobDAOImp implements JobDAO{

    private CqlSession cqlSession;
    
    @Inject
    public JobDAOImp(CqlSession cqlSession) {
        this.cqlSession = cqlSession;
    }

    @Override
    public void createJob(JobRequest request) {
        JobDTO jobDTO = JobDTO.builder()
            .jobId(request.getJobId())
            .objectLink("test")
            .status(Status.SUCCESS)
            .timeStamp("test")
            .build();

        
        String write = "INSERT INTO jobs (jobId, status, linkToObject, timeStamp, metadata) VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = cqlSession.prepare(write);

        BoundStatement bs = ps.bind(jobDTO.getJobId(), jobDTO.getStatus().toValue(), jobDTO.getObjectLink(), jobDTO.getTimeStamp(), jobDTO.getMetadata());
        cqlSession.execute(bs);
    }

    @Override
    public Optional<JobDTO> findByJobId(String jobId) {

        PreparedStatement stmt = cqlSession.prepare(
            "SELECT jobId, status, linkToObject, timeStamp, metadata FROM jobs WHERE jobId = ?"
        );
        
        BoundStatement bound = stmt.bind(jobId);
        ResultSet rs = cqlSession.execute(bound);
        Row row = rs.one();

        JobDTO jobDTO = JobDTO.builder()
            .jobId(row.getString("jobId"))
            .objectLink(row.getString("linkToObject"))
            .status(Status.fromValue(row.getString("status")))
            .timeStamp(row.getString("timeStamp"))
            .metadata(row.getMap("metadata", String.class, String.class))
            .build();

        return Optional.ofNullable(jobDTO);
    }


}
