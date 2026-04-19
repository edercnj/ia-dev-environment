package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewGate")
class ReviewGateTest {

    private final ReviewGate gate = new ReviewGate();

    @Test
    @DisplayName("database configured includes x-review-db")
    void evaluate_database_includesReviewDb() {
        ProjectConfig config = TestConfigBuilder.builder()
                .database("postgres", "16")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-review-db");
    }

    @Test
    @DisplayName("observability configured includes"
            + " x-review-obs")
    void evaluate_observability_includesReviewObs() {
        ProjectConfig config = TestConfigBuilder.builder()
                .observabilityTool("otel")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-review-obs");
    }

    @Test
    @DisplayName("container configured includes"
            + " x-review-devops")
    void evaluate_container_includesReviewDevops() {
        ProjectConfig config = TestConfigBuilder.builder()
                .container("docker")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-review-devops");
    }

    @Test
    @DisplayName("database + hexagonal style includes"
            + " x-review-data-modeling")
    void evaluate_dbHexagonal_includesDataModeling() {
        ProjectConfig config = TestConfigBuilder.builder()
                .database("postgres", "16")
                .architectureStyle("hexagonal")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-review-data-modeling");
    }

    @Test
    @DisplayName("database + layered style excludes"
            + " x-review-data-modeling")
    void evaluate_dbLayered_excludesDataModeling() {
        ProjectConfig config = TestConfigBuilder.builder()
                .database("postgres", "16")
                .architectureStyle("layered")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills)
                .doesNotContain("x-review-data-modeling");
    }

    @Test
    @DisplayName("all defaults (none) returns empty")
    void evaluate_allNone_returnsEmpty() {
        ProjectConfig config = TestConfigBuilder.builder()
                .database("none", "")
                .container("none")
                .observabilityTool("none")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).isEmpty();
    }

    @Test
    @DisplayName("isHexagonalOrDdd true for ddd style")
    void isHexagonalOrDdd_ddd_returnsTrue() {
        ProjectConfig config = TestConfigBuilder.builder()
                .architectureStyle("ddd")
                .build();

        assertThat(ReviewGate.isHexagonalOrDdd(config))
                .isTrue();
    }

    @Test
    @DisplayName("isHexagonalOrDdd false for layered style")
    void isHexagonalOrDdd_layered_returnsFalse() {
        ProjectConfig config = TestConfigBuilder.builder()
                .architectureStyle("layered")
                .build();

        assertThat(ReviewGate.isHexagonalOrDdd(config))
                .isFalse();
    }
}
