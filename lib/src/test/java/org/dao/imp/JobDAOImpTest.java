package org.dao.imp;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.core.servererrors.UnavailableException;

import org.dao.clients.CassandraClient;
import org.dao.exceptions.dDBReadFailedException;
import org.dao.exceptions.dDBWriteFailedException;
import org.dao.models.JobDTO;
import org.dao.models.JobRequest;
import org.dao.models.JobSummaryDTO;
import org.dao.models.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobDAOImpTest {

    private static final String JOB_ID = "1";
    private static final String REQUEST_ID = "1";
    private static final String LINK = "s3://STEFANUNGEREAN@HOTMAIL.COM";
    private static final Instant TIMESTAMP = Instant.now();
    private static final String FILE_HASH = "1";

    @Mock
    private CassandraClient cassandraClient;

    @Mock
    private CqlSession cqlSession;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private BoundStatement boundStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private Row row;

    @InjectMocks
    private JobDAOImp jobDAO;

    private JobRequest request;

    @BeforeEach
    void setUp() {
        request = JobRequest.builder()
                .jobId(JOB_ID)
                .timeStamp(TIMESTAMP)
                .requestId(REQUEST_ID)
                .fileHash(FILE_HASH)
                .build();
    }

    // ----------- createJob ------------------
    @Test
    void createJob_success() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(boundStatement);

        assertDoesNotThrow(() -> jobDAO.createJob(request));

        verify(preparedStatement).bind(JOB_ID, "INPROGRESS", null, null, TIMESTAMP.toString(), null, FILE_HASH, null);
        verify(cqlSession).execute(boundStatement);
    }

    @Test
    void createJob_bindsInputObjectKeyAndAnimalType() {
        request.setInputObjectKey("uploads/cat.jpg");
        request.setAnimalType("cat");
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(boundStatement);

        jobDAO.createJob(request);

        verify(preparedStatement).bind(JOB_ID, "INPROGRESS", null, "uploads/cat.jpg", TIMESTAMP.toString(), null, FILE_HASH, "cat");
    }

    @Test
    void createJob_failure() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBWriteFailedException.class, () -> jobDAO.createJob(request));
    }

    // ----------- findByJobId ------------------
    @Test
    void findByJobId_success() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);

        when(row.getString("jobId")).thenReturn("job123");
        when(row.getString("resultObjectKey")).thenReturn(LINK);
        when(row.getString("inputObjectKey")).thenReturn(LINK);
        when(row.getString("status")).thenReturn("INPROGRESS");
        when(row.getString("timeStamp")).thenReturn("2023-01-01T00:00:00");
        when(row.getMap(eq("metadata"), eq(String.class), eq(String.class)))
                .thenReturn(Map.of("key", "value"));
        when(row.getString("modelResults")).thenReturn(null);
        when(row.getString("fileHash")).thenReturn(null);
        when(row.getString("userId")).thenReturn(null);
        when(row.getString("animalType")).thenReturn("dog");

        Optional<JobDTO> result = jobDAO.findByJobId("job123");

        assertTrue(result.isPresent());
        assertEquals("job123", result.get().getJobId());
        assertNull(result.get().getUserId());
        assertEquals("dog", result.get().getAnimalType());
    }

    // ----------- createTextOnlyJob ------------------
    @Test
    void createTextOnlyJob_bindsAnimalType() {
        Map<String, String> metadata = Map.of("calculated_triage_tier", "LOW");
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any(), any(), any(), any())).thenReturn(boundStatement);

        jobDAO.createTextOnlyJob(JOB_ID, metadata, "cat");

        verify(preparedStatement).bind(eq(JOB_ID), eq("SUCCESS"), anyString(), eq(metadata), eq("cat"));
        verify(cqlSession).execute(boundStatement);
    }

    @Test
    void createTextOnlyJob_failure_throwsWriteException() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBWriteFailedException.class,
                () -> jobDAO.createTextOnlyJob(JOB_ID, Map.of(), null));
    }

    @Test
    void findByJobId_failure() {
        when(cassandraClient.getSession()).thenThrow(new UnavailableException(null, null, 0, 0));

        assertThrows(dDBReadFailedException.class, () -> jobDAO.findByJobId("job123"));
    }

    // ----------- updateResultObjectKey ------------------
    @Test
    void updateResultObjectKey_success() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any())).thenReturn(boundStatement);

        assertDoesNotThrow(() -> jobDAO.updateResultObjectKey("job123", "s3-pdf-key"));

        verify(cqlSession).execute(boundStatement);
    }

    @Test
    void updateResultObjectKey_failure() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBWriteFailedException.class,
                () -> jobDAO.updateResultObjectKey("job123", "s3-pdf-key"));
    }

    // ----------- updateStatus ------------------
    @Test
    void updateStatus_success() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any())).thenReturn(boundStatement);

        assertDoesNotThrow(() -> jobDAO.updateStatus("job123", Status.SUCCESS));

        verify(cqlSession).execute(boundStatement);
    }

    @Test
    void updateStatus_failure() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBWriteFailedException.class,
                () -> jobDAO.updateStatus("job123", Status.SUCCESS));
    }

    // ----------- hasFileHash ------------------
    @Test
    void hasFileHash_exists() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);

        when(resultSet.one()).thenReturn(row);

        boolean result = jobDAO.hasFileHash("abc123");

        assertTrue(result);
        verify(cassandraClient).getSession();
        verify(cqlSession).execute(boundStatement);
    }

    @Test
    void hasFileHash_notExists() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);

        when(resultSet.one()).thenReturn(null);

        boolean result = jobDAO.hasFileHash("abc123");

        assertFalse(result);
    }

    @Test
    void hasFileHash_failure_throwsReadException() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBReadFailedException.class,
                () -> jobDAO.hasFileHash("abc123"));
    }

    @Test
    void hasFileHash_reusesPreparedStatement() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement))
                .thenReturn(resultSet);

        when(resultSet.one()).thenReturn(row);
        jobDAO.hasFileHash("hash1");

        when(resultSet.one()).thenReturn(null);
        jobDAO.hasFileHash("hash2");

        verify(cqlSession, times(1)).prepare(anyString());
    }

    // ----------- updateMetadata ------------------

    @Test
    void updateMetadata_success() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any())).thenReturn(boundStatement);

        Map<String, String> metadata = Map.of(
                "survey_age_group", "adult",
                "survey_energy_level", "normal_active",
                "calculated_triage_tier", "LOW"
        );

        assertDoesNotThrow(() -> jobDAO.updateMetadata(JOB_ID, metadata));

        verify(cqlSession).execute(boundStatement);
    }

    @Test
    void updateMetadata_failure_throwsWriteException() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        Map<String, String> metadata = Map.of("calculated_triage_tier", "HIGH");

        assertThrows(dDBWriteFailedException.class, () -> jobDAO.updateMetadata(JOB_ID, metadata));
    }

    // ----------- attributeOwner ------------------

    @Test
    void attributeOwner_success() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any())).thenReturn(boundStatement);
        when(preparedStatement.bind(any(), any(), any())).thenReturn(boundStatement);

        assertDoesNotThrow(() -> jobDAO.attributeOwner("job123", "user-456", "2026-08-16T00:00:00Z"));

        verify(cqlSession).execute(any(BatchStatement.class));
    }

    @Test
    void attributeOwner_failure_throwsWriteException() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBWriteFailedException.class,
                () -> jobDAO.attributeOwner("job123", "user-456", "2026-08-16T00:00:00Z"));
    }

    // ----------- findOwnerByResultObjectKey ------------------

    @Test
    void findOwnerByResultObjectKey_ownedJob_returnsUserId() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);
        when(row.getString("userId")).thenReturn("user-456");

        Optional<String> result = jobDAO.findOwnerByResultObjectKey("s3-key");

        assertTrue(result.isPresent());
        assertEquals("user-456", result.get());
    }

    @Test
    void findOwnerByResultObjectKey_anonymousJob_returnsEmpty() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);
        when(row.getString("userId")).thenReturn(null);

        Optional<String> result = jobDAO.findOwnerByResultObjectKey("s3-key");

        assertTrue(result.isEmpty());
    }

    @Test
    void findOwnerByResultObjectKey_noMatchingJob_returnsEmpty() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(null);

        Optional<String> result = jobDAO.findOwnerByResultObjectKey("missing-key");

        assertTrue(result.isEmpty());
    }

    @Test
    void findOwnerByResultObjectKey_failure_throwsReadException() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBReadFailedException.class,
                () -> jobDAO.findOwnerByResultObjectKey("s3-key"));
    }

    // ----------- findJobsByUser ------------------

    @Test
    void findJobsByUser_returnsSummariesInOrder() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);

        Row row1 = mock(Row.class);
        when(row1.getString("jobId")).thenReturn("job-2");
        when(row1.getString("timeStamp")).thenReturn("2026-08-15T00:00:00Z");
        Row row2 = mock(Row.class);
        when(row2.getString("jobId")).thenReturn("job-1");
        when(row2.getString("timeStamp")).thenReturn("2026-08-01T00:00:00Z");

        when(resultSet.iterator()).thenReturn(List.of(row1, row2).iterator());

        List<JobSummaryDTO> result = jobDAO.findJobsByUser("user-456", 20, null);

        assertEquals(2, result.size());
        assertEquals("job-2", result.get(0).getJobId());
        assertEquals("job-1", result.get(1).getJobId());
        verify(cqlSession).prepare("SELECT jobId, timeStamp FROM job_ks.jobs_by_user WHERE userId = ? LIMIT ?");
        verify(preparedStatement).bind("user-456", 20);
    }

    @Test
    void findJobsByUser_withCursor_queriesOlderThanCursor() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any(), any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);
        when(resultSet.iterator()).thenReturn(Collections.emptyIterator());

        jobDAO.findJobsByUser("user-456", 20, "2026-08-01T00:00:00Z");

        verify(cqlSession).prepare("SELECT jobId, timeStamp FROM job_ks.jobs_by_user WHERE userId = ? AND timeStamp < ? LIMIT ?");
        verify(preparedStatement).bind("user-456", "2026-08-01T00:00:00Z", 20);
    }

    @Test
    void findJobsByUser_empty_returnsEmptyList() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);
        when(resultSet.iterator()).thenReturn(Collections.emptyIterator());

        List<JobSummaryDTO> result = jobDAO.findJobsByUser("user-456", 20, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void findJobsByUser_failure_throwsReadException() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBReadFailedException.class, () -> jobDAO.findJobsByUser("user-456", 20, null));
    }
}