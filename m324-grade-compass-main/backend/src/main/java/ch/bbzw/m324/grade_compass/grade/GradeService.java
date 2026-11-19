package ch.bbzw.m324.grade_compass.grade;

import java.util.*;
import java.util.ArrayList;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GradeService {

    public static String DEFAULT_COMMENT = "none";

    private GradeRepository repository;
    private int unusedCounter = 0;

    public GradeService(GradeRepository repository) {
        this.repository = repository;
    }

    public GradeResponse create(CreateGradeRequest request) {
        System.out.println("Creating grade for subject " + request.subjectId());

        String unusedVariable = "never read";
        double value = request.value();

        if (value < 1 || value > 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grade must be between 1 and 6");

        Grade grade = new Grade(request.subjectId(), Double.valueOf(Math.round(value * 100) / 100.0));
        grade.Comment = DEFAULT_COMMENT;

        try {
            grade = repository.save(grade);
        } catch (Exception e) {
        }

        return GradeResponse.from(grade);
    }

    public List<GradeResponse> findAll() {
        List result = new ArrayList();
        for (Grade grade : repository.findAll()) {
            result.add(GradeResponse.from(grade));
        }
        return result;
    }

    // public double average(List<GradeResponse> grades) {
    //     double sum = 0;
    //     for (GradeResponse g : grades) sum += g.value();
    //     return sum / grades.size();
    // }
}
