package dev.iadev.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PipelineResult")
class PipelineResultTest {

    @Test
    @DisplayName("stores all fields correctly")
    void constructor_allFields_storedCorrectly() {
        var result = new PipelineResult(
                true,
                "/output",
                List.of("file1.md", "file2.md"),
                List.of("warn1"),
                1500L);

        assertThat(result.success()).isTrue();
        assertThat(result.outputDir()).isEqualTo("/output");
        assertThat(result.filesGenerated())
                .containsExactly("file1.md", "file2.md");
        assertThat(result.warnings()).containsExactly("warn1");
        assertThat(result.durationMs()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("filesGenerated is immutable")
    void filesGenerated_immutable_throwsOnModification() {
        var result = new PipelineResult(
                true, "/out", List.of("f1"), List.of(), 100L);

        assertThatThrownBy(
                () -> result.filesGenerated().add("f2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("warnings is immutable")
    void warnings_immutable_throwsOnModification() {
        var result = new PipelineResult(
                true, "/out", List.of(), List.of("w1"), 100L);

        assertThatThrownBy(
                () -> result.warnings().add("w2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("defensive copy prevents mutation via original list")
    void defensiveCopy_originalMutation_ignored() {
        var mutableFiles = new ArrayList<>(List.of("f1"));
        var result = new PipelineResult(
                true, "/out", mutableFiles, List.of(), 100L);
        mutableFiles.add("f2");

        assertThat(result.filesGenerated()).containsExactly("f1");
    }
}
