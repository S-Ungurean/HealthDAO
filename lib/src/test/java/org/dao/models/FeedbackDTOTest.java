package org.dao.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FeedbackDTOTest {

    private static final String FEEDBACK_ID = "feedback-uuid-123";
    private static final String JOB_ID = "job-123";
    private static final int STAR_RATING = 4;
    private static final String COMMENTS = "Looks good";
    private static final String TIMESTAMP = "2025-01-01T00:00:00Z";

    @Test
    void testBuilderCreatesObject() {
        FeedbackDTO dto = FeedbackDTO.builder()
                .feedbackId(FEEDBACK_ID)
                .jobId(JOB_ID)
                .starRating(STAR_RATING)
                .comments(COMMENTS)
                .timeStamp(TIMESTAMP)
                .build();

        assertNotNull(dto);
        assertEquals(FEEDBACK_ID, dto.getFeedbackId());
        assertEquals(JOB_ID, dto.getJobId());
        assertEquals(STAR_RATING, dto.getStarRating());
        assertEquals(COMMENTS, dto.getComments());
        assertEquals(TIMESTAMP, dto.getTimeStamp());
    }

    @Test
    void testSetterAndGetter() {
        FeedbackDTO dto = new FeedbackDTO();
        dto.setFeedbackId(FEEDBACK_ID);
        dto.setJobId(JOB_ID);
        dto.setStarRating(STAR_RATING);
        dto.setComments(COMMENTS);
        dto.setTimeStamp(TIMESTAMP);

        assertEquals(FEEDBACK_ID, dto.getFeedbackId());
        assertEquals(JOB_ID, dto.getJobId());
        assertEquals(STAR_RATING, dto.getStarRating());
        assertEquals(COMMENTS, dto.getComments());
        assertEquals(TIMESTAMP, dto.getTimeStamp());
    }

    @Test
    void testNonNullEnforced() {
        assertThrows(NullPointerException.class, () ->
                FeedbackDTO.builder()
                        .feedbackId(null)
                        .jobId(JOB_ID)
                        .timeStamp(TIMESTAMP)
                        .build());

        assertThrows(NullPointerException.class, () ->
                FeedbackDTO.builder()
                        .feedbackId(FEEDBACK_ID)
                        .jobId(null)
                        .timeStamp(TIMESTAMP)
                        .build());

        assertThrows(NullPointerException.class, () ->
                FeedbackDTO.builder()
                        .feedbackId(FEEDBACK_ID)
                        .jobId(JOB_ID)
                        .timeStamp(null)
                        .build());
    }
}
