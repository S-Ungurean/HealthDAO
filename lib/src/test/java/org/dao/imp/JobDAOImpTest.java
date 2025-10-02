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
import org.dao.models.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
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
                .build();
    }

    // ----------- createJob ------------------
    @Test
    void createJob_success() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any(), any(), any(), any(), any())).thenReturn(boundStatement);

        assertDoesNotThrow(() -> jobDAO.createJob(request));

        verify(cqlSession).execute(boundStatement);
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

        Optional<JobDTO> result = jobDAO.findByJobId("job123");

        assertTrue(result.isPresent());
        assertEquals("job123", result.get().getJobId());
    }

    @Test
    void findByJobId_failure() {
        when(cassandraClient.getSession()).thenThrow(new UnavailableException(null, null, 0, 0));

        assertThrows(dDBReadFailedException.class, () -> jobDAO.findByJobId("job123"));
    }

    // ----------- updateLinkToObjectAndStatus ------------------
    @Test
    void updateLinkToObjectAndStatus_success() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any(), any())).thenReturn(boundStatement);

        assertDoesNotThrow(() -> jobDAO.updateResultObjectKeyAndStatus("job123", "link", Status.INPROGRESS));

        verify(cqlSession).execute(boundStatement);
    }

    @Test
    void updateLinkToObjectAndStatus_failure() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBWriteFailedException.class,
                () -> jobDAO.updateResultObjectKeyAndStatus("job123", "link", Status.INPROGRESS));
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

    @Test
    void updateInputObjectKey_success() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any())).thenReturn(boundStatement);

        assertDoesNotThrow(() -> jobDAO.updateInputObjectKey("job123", "link"));

        verify(cqlSession).execute(boundStatement);
    }

    @Test
    void updateInputObjectKey_failure() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBWriteFailedException.class,
                () -> jobDAO.updateInputObjectKey("job123", "link"));
    }
}