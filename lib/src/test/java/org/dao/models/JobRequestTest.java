package org.dao.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class JobRequestTest {

    @Test
    void testBuilderCreatesObject() {
        JobRequest jobRequest = JobRequest.builder()
                                          .jobId("1")
                                          .build();

        assertNotNull(jobRequest);
        assertEquals("1", jobRequest.getJobId());
    }

    @Test
    void testSetterAndGetter() {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setJobId("1");

        assertEquals("1", jobRequest.getJobId());
    }

    @Test
    void testNonNullEnforced() {
        assertThrows(NullPointerException.class, () -> {
            JobRequest.builder().jobId(null).build();
        });
    }
}
