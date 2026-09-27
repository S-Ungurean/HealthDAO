package org.dao.imp;

import java.util.Optional;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import org.dao.clients.CassandraClient;
import org.dao.exceptions.dDBReadFailedException;
import org.dao.exceptions.dDBWriteFailedException;
import org.dao.models.UserDTO;

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
public class UserDAOImpTest {

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
    private UserDAOImp userDAO;

    private UserDTO candidate;

    @BeforeEach
    void setUp() {
        candidate = UserDTO.builder()
                .userId("user-123")
                .googleSub("google-sub-abc")
                .email("owner@example.com")
                .displayName("Pet Owner")
                .pictureUrl("https://example.com/pic.png")
                .createdAt("2026-08-16T00:00:00Z")
                .updatedAt("2026-08-16T00:00:00Z")
                .build();
    }

    @Test
    void findByGoogleSub_found_resolvesFullProfile() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);
        when(row.getString("userId")).thenReturn("user-123");
        when(row.getString("googleSub")).thenReturn("google-sub-abc");
        when(row.getString("email")).thenReturn("owner@example.com");
        when(row.getString("displayName")).thenReturn("Pet Owner");
        when(row.getString("pictureUrl")).thenReturn("https://example.com/pic.png");
        when(row.getString("createdAt")).thenReturn("2026-08-16T00:00:00Z");
        when(row.getString("updatedAt")).thenReturn("2026-08-16T00:00:00Z");

        Optional<UserDTO> result = userDAO.findByGoogleSub("google-sub-abc");

        assertTrue(result.isPresent());
        assertEquals("user-123", result.get().getUserId());
        assertEquals("owner@example.com", result.get().getEmail());
    }

    @Test
    void findByGoogleSub_notFound_returnsEmpty() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(null);

        Optional<UserDTO> result = userDAO.findByGoogleSub("nonexistent-sub");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserId_notFound_returnsEmpty() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(null);

        Optional<UserDTO> result = userDAO.findByUserId("missing-user");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserId_clusterUnavailable_throwsReadException() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBReadFailedException.class, () -> userDAO.findByUserId("user-123"));
    }

    @Test
    void createUser_firstLogin_insertsAndReturnsCandidate() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        // LWT insert into users_by_google_sub: bind(googleSub, userId)
        when(preparedStatement.bind(any(), any())).thenReturn(boundStatement);
        // Insert into users: bind(userId, googleSub, email, displayName, pictureUrl, createdAt, updatedAt)
        when(preparedStatement.bind(any(), any(), any(), any(), any(), any(), any())).thenReturn(boundStatement);
        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);
        when(row.getBoolean("[applied]")).thenReturn(true);

        UserDTO result = userDAO.createUser(candidate);

        assertEquals(candidate.getUserId(), result.getUserId());
        verify(cqlSession, times(2)).execute(any(BoundStatement.class));
    }

    @Test
    void createUser_concurrentFirstLoginRace_returnsExistingUser() {
        when(cassandraClient.getSession()).thenReturn(cqlSession);
        when(cqlSession.prepare(anyString())).thenReturn(preparedStatement);
        // LWT insert bind(googleSub, userId) -> not applied
        when(preparedStatement.bind(any(), any())).thenReturn(boundStatement);
        // findByUserId bind(userId) for the fallback read
        when(preparedStatement.bind(any())).thenReturn(boundStatement);

        Row lwtRow = mock(Row.class);
        when(lwtRow.getBoolean("[applied]")).thenReturn(false);
        when(lwtRow.getString("userId")).thenReturn("existing-user-999");

        Row userRow = mock(Row.class);
        when(userRow.getString("userId")).thenReturn("existing-user-999");
        when(userRow.getString("googleSub")).thenReturn(candidate.getGoogleSub());
        when(userRow.getString("email")).thenReturn("other@example.com");
        when(userRow.getString("displayName")).thenReturn("Existing User");
        when(userRow.getString("pictureUrl")).thenReturn("https://example.com/existing.png");
        when(userRow.getString("createdAt")).thenReturn("2026-08-01T00:00:00Z");
        when(userRow.getString("updatedAt")).thenReturn("2026-08-01T00:00:00Z");

        when(cqlSession.execute(boundStatement)).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(lwtRow, userRow);

        UserDTO result = userDAO.createUser(candidate);

        assertEquals("existing-user-999", result.getUserId());
        assertEquals("other@example.com", result.getEmail());
        verify(cqlSession, times(2)).execute(boundStatement);
    }

    @Test
    void createUser_writeFailure_throwsWriteException() {
        when(cassandraClient.getSession()).thenThrow(new NoNodeAvailableException());

        assertThrows(dDBWriteFailedException.class, () -> userDAO.createUser(candidate));
    }
}
