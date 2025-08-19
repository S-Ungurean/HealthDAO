package org.dao.imp;

import java.util.Optional;

import org.dao.JobDAO;
import org.dao.clients.CassandraClient;
import org.dao.exceptions.dDBReadFailedException;
import org.dao.exceptions.dDBWriteFailedException;
import org.dao.models.JobDTO;
import org.dao.models.JobRequest;
import org.dao.models.Status;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.servererrors.QueryValidationException;
import com.datastax.oss.driver.api.core.servererrors.ReadTimeoutException;
import com.datastax.oss.driver.api.core.servererrors.UnavailableException;
import com.datastax.oss.driver.api.core.servererrors.WriteTimeoutException;

import jakarta.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class JobDAOImp implements JobDAO {

    private static String insertStatement = "INSERT INTO job_ks.jobs (jobId, status, linkToObject, timeStamp, metadata) VALUES (?, ?, ?, ?, ?)";
    private static String queryByJobIdStatement = "SELECT jobId, status, linkToObject, timeStamp, metadata FROM job_ks.jobs WHERE jobId = ?";
    private static String updateLinkToObjectStatement = "UPDATE job_ks.jobs SET linkToObject = ?, status = ? WHERE jobId = ?";
    private static String updateStatusStatement = "UPDATE job_ks.jobs SET status = ? WHERE jobId = ?";

    private CassandraClient cassandraClient;
    
    @Inject
    public JobDAOImp(CassandraClient cassandraClient) {
        this.cassandraClient = cassandraClient;
    }

    @Override
    public void createJob(JobRequest request) {
        log.info("Writing record with id: " + request.getJobId());
        
        JobDTO jobDTO = JobDTO.builder()
            .jobId(request.getJobId())
            .timeStamp(request.getTimeStamp().toString())
            .build();
        
        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement insert = cqlSession.prepare(insertStatement);
            jobDTO.getStatus();
            BoundStatement bs = insert.bind(
                jobDTO.getJobId(),
                Status.safeToValue(null),
                jobDTO.getObjectLink(),
                jobDTO.getTimeStamp(),
                jobDTO.getMetadata());
            cqlSession.execute(bs);
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Failed to write record with jobId: " + request.getJobId());
            throw new dDBWriteFailedException("Failed to write job record");
        } catch (QueryValidationException e) {
            log.error("Failed to write record with jobId: " + request.getJobId());
            throw new dDBWriteFailedException("Failed to write job record");
        } catch (DriverException e) {
            log.error("Failed to write record with jobId: " + request.getJobId());
            throw new dDBWriteFailedException("Failed to write job record");
        } catch (NullPointerException e) {
            log.error("Failed to write record with jobId: " + request.getJobId());
            throw new dDBWriteFailedException("Failed to write job record");
        }
    }

    @Override
    public Optional<JobDTO> findByJobId(String jobId) {
        log.info("Querying record with id: " + jobId);

        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement queryByJobId = cqlSession.prepare(queryByJobIdStatement);
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
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Cluster availability issue while writing jobId {}", jobId, e);
            throw new dDBReadFailedException("Cluster unavailable for job write", e);
        } catch (QueryValidationException e) {
            log.error("Invalid query for jobId {}: {}", jobId, e.getMessage(), e);
            throw new dDBReadFailedException("Invalid query for job write", e);
        } catch (DriverException e) {
            log.error("Unexpected Cassandra driver error for jobId {}", jobId, e);
            throw new dDBReadFailedException("Unexpected Cassandra error", e);
        } catch (NullPointerException e) {
            log.error("Failed to read record with jobId: " + jobId);
            throw new dDBReadFailedException("Failed to read job record");
        }
    }

    @Override
    public void updateLinkToObjectAndStatus(String jobId, String objectLink, Status status) {
        log.info("Updating link to object for record with id: " + jobId);

        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement updateLinkToObject = cqlSession.prepare(updateLinkToObjectStatement);
            BoundStatement bound = updateLinkToObject.bind(objectLink, status.toValue(), jobId);
            cqlSession.execute(bound);
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Cluster availability issue while writing jobId {}", jobId, e);
            throw new dDBWriteFailedException("Cluster unavailable for job write", e);
        } catch (QueryValidationException e) {
            log.error("Invalid query for jobId {}: {}", jobId, e.getMessage(), e);
            throw new dDBWriteFailedException("Invalid query for job write", e);
        } catch (DriverException e) {
            log.error("Unexpected Cassandra driver error for jobId {}", jobId, e);
            throw new dDBWriteFailedException("Unexpected Cassandra error", e);
        } catch (NullPointerException e) {
            log.error("Failed to write record with jobId: " + jobId);
            throw new dDBWriteFailedException("Failed to write job record");
        }
    }

    @Override
    public void updateStatus(String jobId, Status status) {
        log.info("Updating link to object for record with id: " + jobId);

        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement updateStatus = cqlSession.prepare(updateStatusStatement);
            BoundStatement bound = updateStatus.bind(status.toValue(), jobId);
            cqlSession.execute(bound);
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Cluster availability issue while writing jobId {}", jobId, e);
            throw new dDBWriteFailedException("Cluster unavailable for job write", e);
        } catch (QueryValidationException e) {
            log.error("Invalid query for jobId {}: {}", jobId, e.getMessage(), e);
            throw new dDBWriteFailedException("Invalid query for job write", e);
        } catch (DriverException e) {
            log.error("Unexpected Cassandra driver error for jobId {}", jobId, e);
            throw new dDBWriteFailedException("Unexpected Cassandra error", e);
        } catch (NullPointerException e) {
            log.error("Failed to write record with jobId: " + jobId);
            throw new dDBWriteFailedException("Failed to write job record");
        }
    }
}
