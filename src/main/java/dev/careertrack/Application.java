package dev.careertrack;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Application(UUID id, String company, String role, String location,
        Stage stage, LocalDate appliedOn, LocalDate interviewOn, String notes,
        int version, Instant createdAt) {
    public enum Stage { SAVED, APPLIED, INTERVIEW, OFFER, CLOSED }
}
