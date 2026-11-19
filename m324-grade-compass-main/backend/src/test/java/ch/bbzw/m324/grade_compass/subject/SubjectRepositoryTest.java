package ch.bbzw.m324.grade_compass.subject;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class SubjectRepositoryTest {

    @Autowired
    private SubjectRepository repository;

    @Test
    void save_assignsIdAndPersistsName() {
        Subject saved = repository.saveAndFlush(new Subject("Mathe"));

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(repository.findById(saved.getId()))
                .get()
                .extracting(Subject::getName)
                .isEqualTo("Mathe");
    }

    @Test
    void existsByNameIgnoreCase_isTrueForSameName() {
        repository.saveAndFlush(new Subject("Mathe"));

        assertThat(repository.existsByNameIgnoreCase("Mathe")).isTrue();
    }

    @Test
    void existsByNameIgnoreCase_ignoresCase() {
        repository.saveAndFlush(new Subject("Mathe"));

        assertThat(repository.existsByNameIgnoreCase("mathe")).isTrue();
        assertThat(repository.existsByNameIgnoreCase("MATHE")).isTrue();
    }

    @Test
    void existsByNameIgnoreCase_isFalseForUnknownName() {
        repository.saveAndFlush(new Subject("Mathe"));

        assertThat(repository.existsByNameIgnoreCase("Deutsch")).isFalse();
    }

    @Test
    void existsByNameIgnoreCase_isFalseOnEmptyTable() {
        assertThat(repository.existsByNameIgnoreCase("Mathe")).isFalse();
    }

    @Test
    void save_duplicateName_violatesUniqueConstraint() {
        repository.saveAndFlush(new Subject("Mathe"));

        assertThatThrownBy(() -> repository.saveAndFlush(new Subject("Mathe")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Documents a known limitation: the DB constraint is case-sensitive, so only the
     * service-level check (existsByNameIgnoreCase) prevents "Mathe" and "mathe" from coexisting.
     */
    @Test
    void save_nameDifferingOnlyInCase_isNotBlockedByDatabaseConstraint() {
        repository.saveAndFlush(new Subject("Mathe"));
        repository.saveAndFlush(new Subject("mathe"));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void save_nullName_violatesNotNullConstraint() {
        assertThatThrownBy(() -> repository.saveAndFlush(new Subject(null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
