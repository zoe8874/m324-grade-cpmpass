package ch.bbzw.m324.grade_compass.grade;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.io.IOException;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private GradeService service;

    public GradeController(GradeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradeResponse create(@Valid @RequestBody CreateGradeRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<GradeResponse> list() {
        return service.findAll();
    }
}
