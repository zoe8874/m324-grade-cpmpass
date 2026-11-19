package ch.bbzw.m324.grade_compass.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
        @NotBlank @Size(max = 100) String name
) {
}
