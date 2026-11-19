package ch.bbzw.m324.grade_compass.grade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeControllerTest {

@Mock
private GradeRepository repository;

@InjectMocks
private GradeController service;
//POST /api/grades mit gültigem Body liefert 201 und die erstellte Note
@Test
void create_savesGradeAndReturnsResponse(){
    when(repository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

    GradeResponse response = service.create(new CreateGradeRequest(1L, 5.5));

    assertThat(response.subjectId()).isEqualTo(1L);
    assertThat(response.value()).isEqualTo(5.5);
    verify(repository).save(any(Grade.class));
}
//POST /api/grades ohne subjectId oder ohne value liefert 400, der Service wird nicht aufgerufen
@Test
void create_badreQuestesdontsaveAndReturnsRespnse() {
    assertThatThrownBy(() -> service.create(new CreateGradeRequest(1L, 7.0)))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                    e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

    verify(repository, never()).save(any());


}
//GET /api/grades liefert alle Noten
@Test
void findAllGrades_mapsEntitiesToResponses() {
    when(repository.findAll()).thenReturn(List.of(new Grade(1L, 4.0), new Grade(2L, 5.5)));

    List<GradeResponse> result = service.findAll();

    assertThat(result).extracting(GradeResponse::value).containsExactly(4.0, 5.5);
}


