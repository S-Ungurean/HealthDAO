package org.dao.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FeedbackRequestTest {

    @Test
    void testBuilderCreatesObject() {
        FeedbackRequest request = FeedbackRequest.builder()
                .jobId("job-123")
                .starRating(5)
                .comments("Great result")
                .build();

        assertNotNull(request);
        assertEquals("job-123", request.getJobId());
        assertEquals(5, request.getStarRating());
        assertEquals("Great result", request.getComments());
    }

    @Test
    void testSetterAndGetter() {
        FeedbackRequest request = new FeedbackRequest();
        request.setJobId("job-456");
        request.setStarRating(3);
        request.setComments("Could be better");

        assertEquals("job-456", request.getJobId());
        assertEquals(3, request.getStarRating());
        assertEquals("Could be better", request.getComments());
    }

    @Test
    void testNonNullEnforced() {
        assertThrows(NullPointerException.class, () ->
                FeedbackRequest.builder()
                        .jobId(null)
                        .build());
    }

    @Test
    void testCommentsIsOptional() {
        assertDoesNotThrow(() ->
                FeedbackRequest.builder()
                        .jobId("job-123")
                        .starRating(1)
                        .build());
    }
}
