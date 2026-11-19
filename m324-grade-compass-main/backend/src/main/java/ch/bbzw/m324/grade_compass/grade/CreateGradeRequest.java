package ch.bbzw.m324.grade_compass.grade;

import jakarta.validation.constraints.NotNull;

public record CreateGradeRequest(
        @NotNull Long subjectId,
        @NotNull Double value
) {
}
