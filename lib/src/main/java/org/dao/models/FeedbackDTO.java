package org.dao.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDTO {

    @NonNull
    private String feedbackId;

    @NonNull
    private String jobId;

    private String correctness;

    private int starRating;

    private String comments;

    @NonNull
    private String timeStamp;
}
