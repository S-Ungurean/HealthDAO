package org.dao.imp;

import java.util.Optional;

import org.dao.JobDAO;
import org.dao.clients.CassandraClient;
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

    private static String insertStatement = "INSERT INTO job_ks.jobs (jobId, status, linkToObject, timeStamp, metadata) VALUES (?, ?, ?, ?, ?)";
    private static String queryByJobIdStatement = "SELECT jobId, status, linkToObject, timeStamp, metadata FROM job_ks.jobs WHERE jobId = ?";
    private static String updateLinkToObjectStatement = "UPDATE job_ks.jobs SET linkToObject = ? WHERE jobId = ?";

    private CassandraClient cassandraClient;
    private final PreparedStatement insert;
    private final PreparedStatement queryByJobId;
    private final PreparedStatement updateLinkToObject;
    
    @Inject
    public JobDAOImp(CassandraClient cassandraClient) {
        this.cassandraClient = cassandraClient;
        this.insert = cassandraClient.getSession().prepare(insertStatement);
        this.queryByJobId = cassandraClient.getSession().prepare(queryByJobIdStatement);
        this.updateLinkToObject = cassandraClient.getSession().prepare(updateLinkToObjectStatement);
    }

    @Override
    public void createJob(JobRequest request) {
        CqlSession cqlSession = cassandraClient.getSession();

        JobDTO jobDTO = JobDTO.builder()
            .jobId(request.getJobId())
            .objectLink(null)
            .status(Status.SUCCESS)
            .timeStamp("test")
            .metadata(null)
            .build();

        BoundStatement bs = insert.bind(
            jobDTO.getJobId(),
            jobDTO.getStatus().toValue(),
            jobDTO.getObjectLink(),
            jobDTO.getTimeStamp(),
            jobDTO.getMetadata());

        cqlSession.execute(bs);
    }

    @Override
    public Optional<JobDTO> findByJobId(String jobId) {
        CqlSession cqlSession = cassandraClient.getSession();
        
        BoundStatement bound = queryByJobId.bind(jobId);
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

    @Override
    public void updateLinkToObject(String jobId, String objectLink) {
        CqlSession cqlSession = cassandraClient.getSession();

        BoundStatement bound = updateLinkToObject.bind(objectLink, jobId);
        cqlSession.execute(bound);
    }


}
