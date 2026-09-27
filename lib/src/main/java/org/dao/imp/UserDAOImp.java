package org.dao.imp;

import java.util.Optional;

import org.dao.UserDAO;
import org.dao.clients.CassandraClient;
import org.dao.exceptions.dDBReadFailedException;
import org.dao.exceptions.dDBWriteFailedException;
import org.dao.models.UserDTO;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.servererrors.QueryValidationException;
import com.datastax.oss.driver.api.core.servererrors.ReadTimeoutException;
import com.datastax.oss.driver.api.core.servererrors.UnavailableException;
import com.datastax.oss.driver.api.core.servererrors.WriteTimeoutException;

import jakarta.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class UserDAOImp implements UserDAO {

    private static String insertUserStatement = "INSERT INTO job_ks.users (userId, googleSub, email, displayName, pictureUrl, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static String queryByUserIdStatement = "SELECT userId, googleSub, email, displayName, pictureUrl, createdAt, updatedAt FROM job_ks.users WHERE userId = ?";
    private static String queryGoogleSubLookupStatement = "SELECT userId FROM job_ks.users_by_google_sub WHERE googleSub = ?";
    private static String insertGoogleSubLookupIfNotExistsStatement = "INSERT INTO job_ks.users_by_google_sub (googleSub, userId) VALUES (?, ?) IF NOT EXISTS";

    private CassandraClient cassandraClient;

    @Inject
    public UserDAOImp(CassandraClient cassandraClient) {
        this.cassandraClient = cassandraClient;
    }

    @Override
    public Optional<UserDTO> findByGoogleSub(String googleSub) {
        log.info("Looking up user by googleSub");
        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement lookup = cqlSession.prepare(queryGoogleSubLookupStatement);
            BoundStatement bound = lookup.bind(googleSub);

            Row row = cqlSession.execute(bound).one();
            if (row == null) {
                return Optional.empty();
            }
            return findByUserId(row.getString("userId"));
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Cluster availability issue while looking up googleSub", e);
            throw new dDBReadFailedException("Cluster unavailable for user lookup", e);
        } catch (QueryValidationException e) {
            log.error("Invalid query for googleSub lookup: {}", e.getMessage(), e);
            throw new dDBReadFailedException("Invalid query for user lookup", e);
        } catch (DriverException e) {
            log.error("Unexpected Cassandra driver error for googleSub lookup", e);
            throw new dDBReadFailedException("Unexpected Cassandra error", e);
        }
    }

    @Override
    public Optional<UserDTO> findByUserId(String userId) {
        log.info("Querying user with id: " + userId);
        try {
            CqlSession cqlSession = cassandraClient.getSession();
            PreparedStatement queryByUserId = cqlSession.prepare(queryByUserIdStatement);
            BoundStatement bound = queryByUserId.bind(userId);

            Row row = cqlSession.execute(bound).one();
            if (row == null) {
                return Optional.empty();
            }

            UserDTO userDTO = UserDTO.builder()
                .userId(row.getString("userId"))
                .googleSub(row.getString("googleSub"))
                .email(row.getString("email"))
                .displayName(row.getString("displayName"))
                .pictureUrl(row.getString("pictureUrl"))
                .createdAt(row.getString("createdAt"))
                .updatedAt(row.getString("updatedAt"))
                .build();

            return Optional.of(userDTO);
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Cluster availability issue while reading userId {}", userId, e);
            throw new dDBReadFailedException("Cluster unavailable for user read", e);
        } catch (QueryValidationException e) {
            log.error("Invalid query for userId {}: {}", userId, e.getMessage(), e);
            throw new dDBReadFailedException("Invalid query for user read", e);
        } catch (DriverException e) {
            log.error("Unexpected Cassandra driver error for userId {}", userId, e);
            throw new dDBReadFailedException("Unexpected Cassandra error", e);
        }
    }

    @Override
    public UserDTO createUser(UserDTO user) {
        log.info("Creating user for googleSub");
        try {
            CqlSession cqlSession = cassandraClient.getSession();

            PreparedStatement lwtInsert = cqlSession.prepare(insertGoogleSubLookupIfNotExistsStatement);
            BoundStatement lwtBound = lwtInsert.bind(user.getGoogleSub(), user.getUserId());
            Row lwtRow = cqlSession.execute(lwtBound).one();

            if (lwtRow != null && !lwtRow.getBoolean("[applied]")) {
                String existingUserId = lwtRow.getString("userId");
                log.info("Concurrent first login detected for googleSub, reusing existing userId");
                return findByUserId(existingUserId)
                    .orElseThrow(() -> new dDBReadFailedException(
                        "users_by_google_sub row exists but users row missing for userId: " + existingUserId));
            }

            PreparedStatement insertUser = cqlSession.prepare(insertUserStatement);
            BoundStatement bs = insertUser.bind(
                user.getUserId(),
                user.getGoogleSub(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPictureUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt());
            cqlSession.execute(bs);
            return user;
        } catch (NoNodeAvailableException | UnavailableException | ReadTimeoutException | WriteTimeoutException e) {
            log.error("Cluster availability issue while creating user", e);
            throw new dDBWriteFailedException("Cluster unavailable for user write", e);
        } catch (QueryValidationException e) {
            log.error("Invalid query for user creation: {}", e.getMessage(), e);
            throw new dDBWriteFailedException("Invalid query for user write", e);
        } catch (DriverException e) {
            log.error("Unexpected Cassandra driver error for user creation", e);
            throw new dDBWriteFailedException("Unexpected Cassandra error", e);
        } catch (NullPointerException e) {
            log.error("Failed to create user");
            throw new dDBWriteFailedException("Failed to write user record");
        }
    }
}
