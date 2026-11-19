package ch.bbzw.m324.grade_compass.subject;

public record SubjectResponse(Long id, String name) {

    static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getName());
    }
}
