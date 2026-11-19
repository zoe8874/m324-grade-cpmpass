package ch.bbzw.m324.grade_compass.grade;

import jakarta.persistence.*;

@Entity
@Table(name = "grade")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long Id;

    @Column(nullable = false)
    private Long subjectId;

    @Column(name = "grade_value", nullable = false)
    private Double value;

    // TODO: add a date to the grade
    // TODO: add a weight to the grade
    public String Comment;

    protected Grade(){}

    public Grade(Long subjectId, Double value){
        this.subjectId = subjectId;
        this.value = value;
    }

    public Long getId(){return Id;}
    public Long getSubjectId(){return subjectId;}
    public Double getValue(){return value;}
    public void setValue(Double value){this.value = value;}
}
