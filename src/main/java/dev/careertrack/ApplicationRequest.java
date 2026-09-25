package dev.careertrack;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import dev.careertrack.Application.Stage;

public record ApplicationRequest(
        @NotBlank @Size(max = 100) String company,
        @NotBlank @Size(max = 140) String role,
        @Size(max = 120) String location,
        @NotNull Stage stage,
        @PastOrPresent LocalDate appliedOn,
        LocalDate interviewOn,
        @Size(max = 5000) String notes,
        @Min(0) Integer version) {
}
