package ch.bbzw.m324.grade_compass.grade;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GradeController.class)
class GradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GradeService service;

    @Test
    void post_validBody_returns201AndCreatedGrade() throws Exception {
        when(service.create(any(CreateGradeRequest.class)))
                .thenReturn(new GradeResponse(1L, 2L, 5.5));

        mockMvc.perform(post("/api/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":2,\"value\":5.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.subjectId").value(2))
                .andExpect(jsonPath("$.value").value(5.5));

        verify(service).create(new CreateGradeRequest(2L, 5.5));
    }

    @Test
    void post_missingSubjectId_returns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":5.5}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void post_missingValue_returns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":2}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void get_returnsAllGrades() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new GradeResponse(1L, 2L, 5.5),
                new GradeResponse(2L, 3L, 4.0)));

        mockMvc.perform(get("/api/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].subjectId").value(2))
                .andExpect(jsonPath("$[0].value").value(5.5))
                .andExpect(jsonPath("$[1].value").value(4.0));

        verify(service).findAll();
    }
}
