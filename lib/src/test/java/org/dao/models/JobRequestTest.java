package org.dao.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JobRequestTest {
    private static JobDTO jobDTO;

    @BeforeAll
    static void setup() {
        jobDTO = JobDTO.builder()
            .JobId(1)
            .status(Status.SUCCESS)
            .timeStamp("timeStamp")
            .ojectLink("link")
            .build();
    }


    @Test
    void testBuilderCreatesObject() {
        JobRequest jobRequest = JobRequest.builder()
                                          .jobDTO(jobDTO)
                                          .build();

        assertNotNull(jobRequest);
        assertEquals(1, jobRequest.getJobDTO().getJobId());
    }

    @Test
    void testSetterAndGetter() {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setJobDTO(jobDTO);

        assertEquals(1, jobRequest.getJobDTO().getJobId());
    }

    @Test
    void testNonNullEnforced() {
        assertThrows(NullPointerException.class, () -> {
            JobRequest.builder().jobDTO(null).build();
        });
    }
}
