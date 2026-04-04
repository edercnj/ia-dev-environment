package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VerificationResult")
class VerificationResultTest {

    @Test
    @DisplayName("stores all fields correctly")
    void constructor_allFields_storedCorrectly() {
        var diff = new FileDiff("f.md", "diff", 10L, 20L);
        var result = new VerificationResult(
                false,
                42,
                List.of(diff),
                List.of("missing.md"),
                List.of("extra.md"));

        assertThat(result.success()).isFalse();
        assertThat(result.totalFiles()).isEqualTo(42);
        assertThat(result.mismatches()).hasSize(1);
        assertThat(result.missingFiles())
                .containsExactly("missing.md");
        assertThat(result.extraFiles())
                .containsExactly("extra.md");
    }

    @Test
    @DisplayName("mismatches list is immutable")
    void mismatches_immutable_throwsOnModification() {
        var result = new VerificationResult(
                true, 0, List.of(), List.of(), List.of());

        assertThatThrownBy(() -> result.mismatches().add(
                new FileDiff("x", "d", 1L, 2L)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("missingFiles list is immutable")
    void missingFiles_immutable_throwsOnModification() {
        var result = new VerificationResult(
                true, 0, List.of(), List.of(), List.of());

        assertThatThrownBy(
                () -> result.missingFiles().add("file.md"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("extraFiles list is immutable")
    void extraFiles_immutable_throwsOnModification() {
        var result = new VerificationResult(
                true, 0, List.of(), List.of(), List.of());

        assertThatThrownBy(
                () -> result.extraFiles().add("extra.md"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("defensive copy prevents mutation")
    void defensiveCopy_originalMutation_ignored() {
        var mutableMissing = new ArrayList<>(List.of("a.md"));
        var result = new VerificationResult(
                true, 0, List.of(), mutableMissing, List.of());
        mutableMissing.add("b.md");

        assertThat(result.missingFiles())
                .containsExactly("a.md");
    }
}
