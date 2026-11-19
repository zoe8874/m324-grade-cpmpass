package ch.bbzw.m324.grade_compass.grade;

public record GradeResponse(Long id, Long subjectId, Double value) {

    static GradeResponse from(Grade grade) {
        return new GradeResponse(grade.getId(), grade.getSubjectId(), grade.getValue());
    }
}
