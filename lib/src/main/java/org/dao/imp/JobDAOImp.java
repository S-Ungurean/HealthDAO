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

    private static String insertStatement = "INSERT INTO job_ks.jobs (jobId, status, resultObjectKey, inputObjectKey, timeStamp, metadata, fileHash) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static String queryByJobIdStatement = "SELECT jobId, status, resultObjectKey, inputObjectKey, timeStamp, modelResults, metadata, fileHash FROM job_ks.jobs WHERE jobId = ?";
    private static String updateResultObjectKeyAndStatusAndModelResultsStatement = "UPDATE job_ks.jobs SET resultObjectKey = ?, status = ?, modelResults = ? WHERE jobId = ?";
    private static String updateInputObjectKeyStatement = "UPDATE job_ks.jobs SET inputObjectKey = ? WHERE jobId = ?";
    private static String updateStatusStatement = "UPDATE job_ks.jobs SET status = ? WHERE jobId = ?";
    private static String checkFileHashStatement = "SELECT jobId FROM job_ks.jobs WHERE fileHash = ? LIMIT 1";

    private PreparedStatement psCheckFileHash;

    private CassandraClient cassandraClient;
    
    @Inject
    public JobDAOImp(CassandraClient cassandraClient) {
        this.cassandraClient = cassandraClient;
    }

    private PreparedStatement getCheckFileHashStatement(CqlSession session) {
        if (psCheckFileHash == null) {
            psCheckFileHash = session.prepare(checkFileHashStatement);
        }
        return psCheckFileHash;
    }

    @Override
    public void createJob(JobRequest request) {
        log.info("Writing record with id: " + request.getJobId());
        
        JobDTO jobDTO = JobDTO.builder()
            .jobId(request.getJobId())
            .timeStamp(request.getTimeStamp().toString())
            .fileHash(request.getFileHash())
            .build();
        
        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement insert = cqlSession.prepare(insertStatement);
            jobDTO.getStatus();
            BoundStatement bs = insert.bind(
                jobDTO.getJobId(),
                Status.safeToValue(Status.INPROGRESS),
                jobDTO.getResultObjectKey(),
                jobDTO.getInputObjectKey(),
                jobDTO.getTimeStamp(),
                jobDTO.getMetadata(),
                jobDTO.getFileHash());
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
                .resultObjectKey(row.getString("resultObjectKey"))
                .inputObjectKey(row.getString("inputObjectKey"))
                .status(Status.fromValue(row.getString("status")))
                .timeStamp(row.getString("timeStamp"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .modelResults(row.getString("modelResults"))
                .fileHash(row.getString("fileHash"))
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
    public void updateResultObjectKeyAndStatusAndModelResults(String jobId, String objectKey, Status status, String modelResults) {
        log.info("Updating result object key for record with id: " + jobId);

        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement updateResultObjectKey = cqlSession.prepare(updateResultObjectKeyAndStatusAndModelResultsStatement);
            BoundStatement bound = updateResultObjectKey.bind(objectKey, status.toValue(), modelResults, jobId);
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
    public void updateInputObjectKey(String jobId, String objectKey) {
        log.info("Updating input object key for record with id: " + jobId);

        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement updateInputObjectKey = cqlSession.prepare(updateInputObjectKeyStatement);
            BoundStatement bound = updateInputObjectKey.bind(objectKey, jobId);
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
        log.info("Updating status for record with id: " + jobId);

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

    @Override
    public boolean hasFileHash(String fileHash) {
        log.debug("Checking existence of fileHash: " + fileHash);

        try {
            CqlSession cqlSession = cassandraClient.getSession();

            PreparedStatement statement = getCheckFileHashStatement(cqlSession);
            BoundStatement bound = statement.bind(fileHash);

            ResultSet rs = cqlSession.execute(bound);
            
            // rs.one() returns the first row or null if empty. 
            return rs.one() != null;

        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Cluster availability issue while checking fileHash {}", fileHash, e);
            throw new dDBReadFailedException("Cluster unavailable for fileHash check", e);
        } catch (QueryValidationException e) {
            log.error("Invalid query for fileHash {}: {}. Did you create the INDEX?", fileHash, e.getMessage(), e);
            throw new dDBReadFailedException("Invalid query (Missing Index?)", e);
        } catch (DriverException e) {
            log.error("Unexpected Cassandra driver error for fileHash {}", fileHash, e);
            throw new dDBReadFailedException("Unexpected Cassandra error", e);
        }
    }
}
