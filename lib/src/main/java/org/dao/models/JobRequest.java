package org.dao.models;

import java.time.Instant;

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
public class JobRequest {
    @NonNull
    private String jobId;

    @NonNull
    private Instant timeStamp;

    @NonNull
    private String requestId;

    @NonNull
    private String fileHash;
}
