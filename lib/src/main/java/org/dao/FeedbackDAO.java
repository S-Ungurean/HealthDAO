package org.dao;

import org.dao.models.FeedbackRequest;

public interface FeedbackDAO {

    /** Persists user feedback for a completed job. */
    void submitFeedback(FeedbackRequest request);
}
