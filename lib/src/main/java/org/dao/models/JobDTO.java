package org.dao.models;

import java.util.Map;

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
public class JobDTO {
    
    @NonNull
    private String jobId;

    private String objectLink;

    @NonNull
    private Status status;

    @NonNull
    private String timeStamp;

    private Map<String, String> metadata;

}
