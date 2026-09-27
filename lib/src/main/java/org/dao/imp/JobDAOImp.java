package org.dao.imp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.dao.JobDAO;
import org.dao.clients.CassandraClient;
import org.dao.exceptions.dDBReadFailedException;
import org.dao.exceptions.dDBWriteFailedException;
import org.dao.models.JobDTO;
import org.dao.models.JobRequest;
import org.dao.models.JobSummaryDTO;
import org.dao.models.Status;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
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

    private static final int JOBS_BY_USER_LIMIT = 200;

    private static String insertStatement = "INSERT INTO job_ks.jobs (jobId, status, resultObjectKey, inputObjectKey, timeStamp, metadata, fileHash) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static String queryByJobIdStatement = "SELECT jobId, status, resultObjectKey, inputObjectKey, timeStamp, modelResults, metadata, fileHash, userId FROM job_ks.jobs WHERE jobId = ?";
    private static String updateResultObjectKeyStatement = "UPDATE job_ks.jobs SET resultObjectKey = ? WHERE jobId = ?";
    private static String updateStatusStatement = "UPDATE job_ks.jobs SET status = ? WHERE jobId = ?";
    private static String checkFileHashStatement = "SELECT jobId FROM job_ks.jobs WHERE fileHash = ? LIMIT 1";
    private static String updateStatusAndModelResultsStatement = "UPDATE job_ks.jobs SET status = ?, modelResults = ? WHERE jobId = ?";
    private static String updateMetadataStatement = "UPDATE job_ks.jobs SET metadata = metadata + ? WHERE jobId = ?";
    private static String insertTextOnlyJobStatement = "INSERT INTO job_ks.jobs (jobId, status, timeStamp, metadata) VALUES (?, ?, ?, ?)";
    private static String updateOwnerStatement = "UPDATE job_ks.jobs SET userId = ? WHERE jobId = ?";
    private static String insertJobsByUserStatement = "INSERT INTO job_ks.jobs_by_user (userId, timeStamp, jobId) VALUES (?, ?, ?)";
    private static String queryOwnerByResultObjectKeyStatement = "SELECT userId FROM job_ks.jobs WHERE resultObjectKey = ? LIMIT 1";
    private static String queryJobsByUserStatement = "SELECT jobId, timeStamp FROM job_ks.jobs_by_user WHERE userId = ? LIMIT " + JOBS_BY_USER_LIMIT;

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

            if (row == null) {
                return Optional.empty();
            }

            JobDTO jobDTO = JobDTO.builder()
                .jobId(row.getString("jobId"))
                .resultObjectKey(row.getString("resultObjectKey"))
                .inputObjectKey(row.getString("inputObjectKey"))
                .status(Status.fromValue(row.getString("status")))
                .timeStamp(row.getString("timeStamp"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .modelResults(row.getString("modelResults"))
                .fileHash(row.getString("fileHash"))
                .userId(row.getString("userId"))
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
    public void updateResultObjectKey(String jobId, String objectKey) {
        log.info("Updating result object key for record with id: " + jobId);
        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement ps = cqlSession.prepare(updateResultObjectKeyStatement);
            BoundStatement bound = ps.bind(objectKey, jobId);
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
    public void updateStatusAndModelResults(String jobId, Status status, String modelResults) {
        log.info("Updating status and model results for record with id: " + jobId);
        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement ps = cqlSession.prepare(updateStatusAndModelResultsStatement);
            BoundStatement bound = ps.bind(status.toValue(), modelResults, jobId);
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
    public void updateMetadata(String jobId, Map<String, String> metadata) {
        log.info("Updating metadata for record with id: " + jobId);

        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement updateMetadata = cqlSession.prepare(updateMetadataStatement);
            BoundStatement bound = updateMetadata.bind(metadata, jobId);
            cqlSession.execute(bound);
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Cluster availability issue while updating metadata for jobId {}", jobId, e);
            throw new dDBWriteFailedException("Cluster unavailable for metadata update", e);
        } catch (QueryValidationException e) {
            log.error("Invalid query for jobId {}: {}", jobId, e.getMessage(), e);
            throw new dDBWriteFailedException("Invalid query for metadata update", e);
        } catch (DriverException e) {
            log.error("Unexpected Cassandra driver error for jobId {}", jobId, e);
            throw new dDBWriteFailedException("Unexpected Cassandra error", e);
        } catch (NullPointerException e) {
            log.error("Failed to update metadata for jobId: " + jobId);
            throw new dDBWriteFailedException("Failed to update metadata");
        }
    }

    @Override
    public void createTextOnlyJob(String jobId, Map<String, String> metadata) {
        log.info("Writing text-only job record with id: " + jobId);
        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement insert = cqlSession.prepare(insertTextOnlyJobStatement);
            BoundStatement bs = insert.bind(
                jobId,
                Status.safeToValue(Status.SUCCESS),
                Instant.now().toString(),
                metadata);
            cqlSession.execute(bs);
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Failed to write text-only job record with jobId: " + jobId);
            throw new dDBWriteFailedException("Failed to write text-only job record");
        } catch (QueryValidationException e) {
            log.error("Failed to write text-only job record with jobId: " + jobId);
            throw new dDBWriteFailedException("Failed to write text-only job record");
        } catch (DriverException e) {
            log.error("Failed to write text-only job record with jobId: " + jobId);
            throw new dDBWriteFailedException("Failed to write text-only job record");
        } catch (NullPointerException e) {
            log.error("Failed to write text-only job record with jobId: " + jobId);
            throw new dDBWriteFailedException("Failed to write text-only job record");
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

    @Override
    public void attributeOwner(String jobId, String userId, String timeStamp) {
        log.info("Attributing jobId {} to userId", jobId);
        try {
            CqlSession cqlSession = cassandraClient.getSession();

            PreparedStatement updateOwner = cqlSession.prepare(updateOwnerStatement);
            BoundStatement ownerBound = updateOwner.bind(userId, jobId);

            PreparedStatement insertHistory = cqlSession.prepare(insertJobsByUserStatement);
            BoundStatement historyBound = insertHistory.bind(userId, timeStamp, jobId);

            BatchStatement batch = BatchStatement.builder(DefaultBatchType.LOGGED)
                .addStatement(ownerBound)
                .addStatement(historyBound)
                .build();

            cqlSession.execute(batch);
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Cluster availability issue while attributing jobId {}", jobId, e);
            throw new dDBWriteFailedException("Cluster unavailable for owner attribution", e);
        } catch (QueryValidationException e) {
            log.error("Invalid query while attributing jobId {}: {}", jobId, e.getMessage(), e);
            throw new dDBWriteFailedException("Invalid query for owner attribution", e);
        } catch (DriverException e) {
            log.error("Unexpected Cassandra driver error while attributing jobId {}", jobId, e);
            throw new dDBWriteFailedException("Unexpected Cassandra error", e);
        } catch (NullPointerException e) {
            log.error("Failed to attribute jobId: " + jobId);
            throw new dDBWriteFailedException("Failed to attribute job owner");
        }
    }

    @Override
    public Optional<String> findOwnerByResultObjectKey(String resultObjectKey) {
        log.debug("Looking up owner for resultObjectKey");
        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement lookup = cqlSession.prepare(queryOwnerByResultObjectKeyStatement);
            BoundStatement bound = lookup.bind(resultObjectKey);

            Row row = cqlSession.execute(bound).one();
            if (row == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(row.getString("userId"));
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Cluster availability issue while looking up resultObjectKey owner", e);
            throw new dDBReadFailedException("Cluster unavailable for owner lookup", e);
        } catch (QueryValidationException e) {
            log.error("Invalid query for resultObjectKey owner lookup: {}. Did you create the INDEX?", e.getMessage(), e);
            throw new dDBReadFailedException("Invalid query (Missing Index?)", e);
        } catch (DriverException e) {
            log.error("Unexpected Cassandra driver error for resultObjectKey owner lookup", e);
            throw new dDBReadFailedException("Unexpected Cassandra error", e);
        }
    }

    @Override
    public List<JobSummaryDTO> findJobsByUser(String userId) {
        log.info("Querying job history for userId");
        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement queryJobsByUser = cqlSession.prepare(queryJobsByUserStatement);
            BoundStatement bound = queryJobsByUser.bind(userId);

            ResultSet rs = cqlSession.execute(bound);
            List<JobSummaryDTO> summaries = new ArrayList<>();
            for (Row row : rs) {
                summaries.add(JobSummaryDTO.builder()
                    .jobId(row.getString("jobId"))
                    .timeStamp(row.getString("timeStamp"))
                    .build());
            }
            return summaries;
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Cluster availability issue while querying job history for userId", e);
            throw new dDBReadFailedException("Cluster unavailable for job history read", e);
        } catch (QueryValidationException e) {
            log.error("Invalid query for job history: {}", e.getMessage(), e);
            throw new dDBReadFailedException("Invalid query for job history read", e);
        } catch (DriverException e) {
            log.error("Unexpected Cassandra driver error for job history read", e);
            throw new dDBReadFailedException("Unexpected Cassandra error", e);
        }
    }
}
