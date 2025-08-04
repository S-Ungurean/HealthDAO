package org.dao.imp;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import org.dao.clients.CassandraClient;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobDAOImpTest {

    private static final String JOB_ID = "1";
    private static final String REQUEST_ID = "1";
    private static final String LINK = "s3://STEFANUNGEREAN@HOTMAIL.COM";
    private static final String TIMESTAMP = Instant.now().toString();

    @Mock
    private CassandraClient cassandraClient;

    @Mock
    private CqlSession session;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private BoundStatement boundStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private Row row;

    @InjectMocks
    private JobDAOImp jobDAOImp;

    @BeforeEach
    void setup() {
        when(cassandraClient.getSession()).thenReturn(session);
    }
    
    // Create Job
    @Test
    public void testCreateJob() throws Exception {
        JobRequest request = JobRequest.builder()
                .jobId(JOB_ID)
                .requestId(REQUEST_ID)
                .timeStamp(Instant.now())
                .build();
        
        // Mocks for Cassandra
        when(session.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any(), any(), any(), any())).thenReturn(boundStatement);
        when(session.execute(boundStatement)).thenReturn(resultSet);

        assertDoesNotThrow(() -> jobDAOImp.createJob(request));
    }
    
    @Test
    public void testCreateJob_throwsException() throws Exception {
        JobRequest request = JobRequest.builder()
                .jobId(JOB_ID)
                .requestId(REQUEST_ID)
                .timeStamp(Instant.now())
                .build();

        when(session.prepare(anyString())).thenThrow(new RuntimeException("Cassandra prepare failed"));
        Exception exception = assertThrows(Exception.class, () -> jobDAOImp.createJob(request));
        assertEquals("Failed to write job record", exception.getMessage());
    }

    // Find By Job Id
    @Test
    public void testFindByJobId() throws Exception {
        when(session.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(JOB_ID)).thenReturn(boundStatement);
        when(session.execute(boundStatement)).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);

        when(row.getString("jobId")).thenReturn(JOB_ID);
        when(row.getString("status")).thenReturn(Status.SUCCESS.toValue());
        when(row.getString("linkToObject")).thenReturn(LINK);
        when(row.getString("timeStamp")).thenReturn(TIMESTAMP);
        when(row.getMap("metadata", String.class, String.class)).thenReturn(Map.of());

        Optional<JobDTO> result = jobDAOImp.findByJobId(JOB_ID);

        assertTrue(result.isPresent());
        assertEquals(JOB_ID, result.get().getJobId());
        assertEquals(Status.SUCCESS, result.get().getStatus());
    }

    @Test
    public void testFindByJobId_throwsException() throws Exception {
        Exception exception = assertThrows(Exception.class, () -> jobDAOImp.findByJobId(JOB_ID));
        assertEquals("Failed to query job record", exception.getMessage());
    }

    // Update Link To Object And Status
    @Test
    public void testUpdateLinkToObjectAndStatus() throws Exception {
        when(session.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any(), any())).thenReturn(boundStatement);
        when(session.execute(boundStatement)).thenReturn(resultSet);

        assertDoesNotThrow(() -> jobDAOImp.updateLinkToObjectAndStatus(JOB_ID, LINK, Status.SUCCESS));
    }

    @Test
    public void testUpdateLinkToObjectAndStatus_throwsException() throws Exception {
        when(session.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any(), any())).thenReturn(boundStatement);
        when(session.execute(boundStatement)).thenThrow(new RuntimeException("Write error"));

        Exception exception = assertThrows(Exception.class, () ->
            jobDAOImp.updateLinkToObjectAndStatus(JOB_ID, LINK, Status.SUCCESS));

        assertEquals("Failed to update job record", exception.getMessage());
    }

    // Update Status
    @Test
    public void testUpdateStatus() throws Exception {
        when(session.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any())).thenReturn(boundStatement);
        when(session.execute(boundStatement)).thenReturn(resultSet);

        assertDoesNotThrow(() -> jobDAOImp.updateStatus(JOB_ID, Status.SUCCESS));
    }

    @Test
    public void testUpdateStatus_throwsException() throws Exception {
        when(session.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any())).thenReturn(boundStatement);
        when(session.execute(boundStatement)).thenThrow(new RuntimeException("Update error"));

        Exception exception = assertThrows(Exception.class, () ->
            jobDAOImp.updateStatus(JOB_ID, Status.SUCCESS));

        assertEquals("Failed to update job record", exception.getMessage());
    }
}