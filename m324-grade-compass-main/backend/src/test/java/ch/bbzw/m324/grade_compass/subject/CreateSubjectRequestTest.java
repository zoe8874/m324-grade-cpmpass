package ch.bbzw.m324.grade_compass.subject;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateSubjectRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void validName_hasNoViolations() {
        Set<ConstraintViolation<CreateSubjectRequest>> violations =
                validator.validate(new CreateSubjectRequest("Mathematik"));

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n"})
    void nullEmptyOrBlankName_isRejected(String name) {
        Set<ConstraintViolation<CreateSubjectRequest>> violations =
                validator.validate(new CreateSubjectRequest(name));

        assertThat(violations).isNotEmpty();
        assertThat(violations).allSatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("name"));
    }

    @Test
    void nameWithExactly100Characters_isAccepted() {
        Set<ConstraintViolation<CreateSubjectRequest>> violations =
                validator.validate(new CreateSubjectRequest("a".repeat(100)));

        assertThat(violations).isEmpty();
    }

    @Test
    void nameWith101Characters_isRejected() {
        Set<ConstraintViolation<CreateSubjectRequest>> violations =
                validator.validate(new CreateSubjectRequest("a".repeat(101)));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }
}
