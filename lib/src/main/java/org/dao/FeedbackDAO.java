package org.dao;

import org.dao.models.FeedbackRequest;

public interface FeedbackDAO {
    void submitFeedback(FeedbackRequest request);
}
