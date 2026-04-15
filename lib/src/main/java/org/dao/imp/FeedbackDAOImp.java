package org.dao.imp;

import java.time.Instant;
import java.util.UUID;

import org.dao.FeedbackDAO;
import org.dao.clients.CassandraClient;
import org.dao.exceptions.dDBWriteFailedException;
import org.dao.models.FeedbackDTO;
import org.dao.models.FeedbackRequest;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.servererrors.QueryValidationException;
import com.datastax.oss.driver.api.core.servererrors.ReadTimeoutException;
import com.datastax.oss.driver.api.core.servererrors.UnavailableException;
import com.datastax.oss.driver.api.core.servererrors.WriteTimeoutException;

import jakarta.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FeedbackDAOImp implements FeedbackDAO {

    private static String insertStatement = "INSERT INTO job_ks.feedback (feedbackId, jobId, starRating, comments, timeStamp) VALUES (?, ?, ?, ?, ?)";

    private CassandraClient cassandraClient;

    @Inject
    public FeedbackDAOImp(CassandraClient cassandraClient) {
        this.cassandraClient = cassandraClient;
    }

    @Override
    public void submitFeedback(FeedbackRequest request) {
        log.info("Writing feedback for jobId: " + request.getJobId());

        FeedbackDTO feedbackDTO = FeedbackDTO.builder()
            .feedbackId(UUID.randomUUID().toString())
            .jobId(request.getJobId())
            .starRating(request.getStarRating())
            .comments(request.getComments())
            .timeStamp(Instant.now().toString())
            .build();

        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement insert = cqlSession.prepare(insertStatement);
            BoundStatement bs = insert.bind(
                feedbackDTO.getFeedbackId(),
                feedbackDTO.getJobId(),
                feedbackDTO.getStarRating(),
                feedbackDTO.getComments(),
                feedbackDTO.getTimeStamp());
            cqlSession.execute(bs);
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Cluster availability issue while writing feedback for jobId {}", request.getJobId(), e);
            throw new dDBWriteFailedException("Cluster unavailable for feedback write", e);
        } catch (QueryValidationException e) {
            log.error("Invalid query for feedback jobId {}: {}", request.getJobId(), e.getMessage(), e);
            throw new dDBWriteFailedException("Invalid query for feedback write", e);
        } catch (DriverException e) {
            log.error("Unexpected Cassandra driver error for feedback jobId {}", request.getJobId(), e);
            throw new dDBWriteFailedException("Unexpected Cassandra error", e);
        } catch (NullPointerException e) {
            log.error("Failed to write feedback for jobId: " + request.getJobId());
            throw new dDBWriteFailedException("Failed to write feedback record");
        }
    }
}
