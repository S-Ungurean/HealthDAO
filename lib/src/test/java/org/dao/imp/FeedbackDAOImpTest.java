package org.dao.imp;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.servererrors.UnavailableException;

import org.dao.clients.CassandraClient;
import org.dao.exceptions.dDBWriteFailedException;
import org.dao.models.FeedbackRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FeedbackDAOImpTest {

    @Mock
    private CassandraClient cassandraClient;

    @Mock
    private CqlSession cqlSession;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private BoundStatement boundStatement;

    @InjectMocks
    private FeedbackDAOImp feedbackDAO;

    private FeedbackRequest request;

    @BeforeEach
    void setUp() {
        request = FeedbackRequest.builder()
                .jobId("job-123")
                .starRating(4)
                .comments("Good analysis")
                .build();
    }

    @Test
    void submitFeedback_success() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any(), any(), any(), any())).thenReturn(boundStatement);

        assertDoesNotThrow(() -> feedbackDAO.submitFeedback(request));

        verify(cqlSession).execute(boundStatement);
    }

    @Test
    void submitFeedback_noNodeAvailable_throwsWriteException() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBWriteFailedException.class, () -> feedbackDAO.submitFeedback(request));
    }

    @Test
    void submitFeedback_unavailable_throwsWriteException() {
        when(cassandraClient.getSession()).thenThrow(new UnavailableException(null, null, 0, 0));

        assertThrows(dDBWriteFailedException.class, () -> feedbackDAO.submitFeedback(request));
    }

    @Test
    void submitFeedback_nullPointer_throwsWriteException() {
        when(cassandraClient.getSession()).thenReturn(null);

        assertThrows(dDBWriteFailedException.class, () -> feedbackDAO.submitFeedback(request));
    }

    @Test
    void submitFeedback_withNullComments_success() {
        FeedbackRequest noComments = FeedbackRequest.builder()
                .jobId("job-456")
                .starRating(3)
                .build();

        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(), any(), any(), any(), any())).thenReturn(boundStatement);

        assertDoesNotThrow(() -> feedbackDAO.submitFeedback(noComments));

        verify(cqlSession).execute(boundStatement);
    }
}
