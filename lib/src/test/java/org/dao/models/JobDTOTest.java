package org.dao.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JobDTOTest {
    private static String jobId = "1";
    private static String objectLink = "link";
    private static Status status = Status.SUCCESS;
    private static String timeStamp = "timeStamp";
    private static Map<String, String> metadata;

    @BeforeAll
    static void setup() {
        metadata = new HashMap<>();
        metadata.put("modelType", "skinCancer");
        metadata.put("modelVersion", "1.0");
    }

    @Test
    void testBuilderCreatesObject() {
        JobDTO jobDTO = JobDTO.builder()
            .jobId(jobId)
            .resultObjectKey(objectLink)
            .inputObjectKey(objectLink)
            .status(status)
            .timeStamp(timeStamp)
            .metadata(metadata)
            .build();

        assertNotNull(jobDTO);
        assertEquals(jobId, jobDTO.getJobId());
        assertEquals(objectLink, jobDTO.getResultObjectKey());
        assertEquals(objectLink, jobDTO.getInputObjectKey());
        assertEquals(status, jobDTO.getStatus());
        assertEquals(timeStamp, jobDTO.getTimeStamp());
        assertEquals(metadata, jobDTO.getMetadata());
    }

    @Test
    void testSetterAndGetter() {
        JobDTO jobDTO = new JobDTO();
        jobDTO.setJobId(jobId);
        jobDTO.setResultObjectKey(objectLink);
        jobDTO.setInputObjectKey(objectLink);
        jobDTO.setStatus(status);
        jobDTO.setTimeStamp(timeStamp);
        jobDTO.setMetadata(metadata);

        assertEquals(jobId, jobDTO.getJobId());
        assertEquals(objectLink, jobDTO.getResultObjectKey());
        assertEquals(objectLink, jobDTO.getInputObjectKey());
        assertEquals(status, jobDTO.getStatus());
        assertEquals(timeStamp, jobDTO.getTimeStamp());
        assertEquals(metadata, jobDTO.getMetadata()); 
    }

    @Test
    void testNonNullEnforced() {
        assertThrows(NullPointerException.class, () -> {
            JobDTO.builder()
                .jobId(null)
                .resultObjectKey(null)
                .inputObjectKey(null)
                .status(null)
                .timeStamp(null)
                .build();
        });
    }
}
