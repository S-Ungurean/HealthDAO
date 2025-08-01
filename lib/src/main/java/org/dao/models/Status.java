package org.dao.models;

public enum Status {
    SUCCESS,
    FAILED,
    INPROGRESS;

    // Convert enum to string (optional customization)
    public String toValue() {
        return this.name(); // or customize like: this.name().toLowerCase()
    }

    // Convert string to enum (case-insensitive and safe)
    public static Status fromValue(String value) {
        for (Status status : Status.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid Status value: " + value);
    }
}
