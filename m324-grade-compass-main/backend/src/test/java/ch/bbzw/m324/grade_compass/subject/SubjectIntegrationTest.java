package ch.bbzw.m324.grade_compass.subject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubjectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private void createSubject(String name) throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createThenList_showsCreatedSubject() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mathe\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Mathe"));

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Mathe"));
    }

    @Test
    void list_onEmptyDatabase_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void create_duplicateName_returns409AndStoresOnlyOne() throws Exception {
        createSubject("Mathe");

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mathe\"}"))
                .andExpect(status().isConflict());

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void create_duplicateNameWithDifferentCase_returns409() throws Exception {
        createSubject("Mathe");

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"MATHE\"}"))
                .andExpect(status().isConflict());

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void create_nameWithSurroundingWhitespace_isStoredTrimmed() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Mathe  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mathe"));

        assertThat(repository.existsByNameIgnoreCase("Mathe")).isTrue();
    }

    @Test
    void create_blankName_returns400AndStoresNothing() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void create_multipleSubjects_areAllListed() throws Exception {
        createSubject("Mathe");
        createSubject("Deutsch");
        createSubject("Englisch");

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
