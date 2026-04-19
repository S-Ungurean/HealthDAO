package org.dao.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StatusTest {

    @Test
    void toValue_returnsEnumName() {
        assertEquals("SUCCESS", Status.SUCCESS.toValue());
        assertEquals("FAILED", Status.FAILED.toValue());
        assertEquals("INPROGRESS", Status.INPROGRESS.toValue());
    }

    @Test
    void safeToValue_returnsNullForNullInput() {
        assertNull(Status.safeToValue(null));
    }

    @Test
    void safeToValue_returnsValueForNonNull() {
        assertEquals("SUCCESS", Status.safeToValue(Status.SUCCESS));
        assertEquals("FAILED", Status.safeToValue(Status.FAILED));
        assertEquals("INPROGRESS", Status.safeToValue(Status.INPROGRESS));
    }

    @Test
    void fromValue_parsesExactMatch() {
        assertEquals(Status.SUCCESS, Status.fromValue("SUCCESS"));
        assertEquals(Status.FAILED, Status.fromValue("FAILED"));
        assertEquals(Status.INPROGRESS, Status.fromValue("INPROGRESS"));
    }

    @Test
    void fromValue_isCaseInsensitive() {
        assertEquals(Status.SUCCESS, Status.fromValue("success"));
        assertEquals(Status.FAILED, Status.fromValue("failed"));
        assertEquals(Status.INPROGRESS, Status.fromValue("inprogress"));
        assertEquals(Status.SUCCESS, Status.fromValue("Success"));
    }

    @Test
    void fromValue_returnsNullForNullInput() {
        assertNull(Status.fromValue(null));
    }

    @Test
    void fromValue_throwsForUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> Status.fromValue("UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> Status.fromValue(""));
    }
}
