package dev.iadev.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions
        .assertThatThrownBy;

/**
 * Tests for CicdResult — immutable result record with
 * defensive copies.
 */
@DisplayName("CicdResult")
class CicdResultTest {

    @Nested
    @DisplayName("defensive copies (L-007)")
    class DefensiveCopies {

        @Test
        @DisplayName("files list is immutable")
        void create_whenCalled_filesImmutable() {
            CicdResult result = new CicdResult(
                    List.of("file1"), List.of());

            assertThatThrownBy(() ->
                    result.files().add("extra"))
                    .isInstanceOf(
                            UnsupportedOperationException
                                    .class);
        }

        @Test
        @DisplayName("warnings list is immutable")
        void create_whenCalled_warningsImmutable() {
            CicdResult result = new CicdResult(
                    List.of(), List.of("warn1"));

            assertThatThrownBy(() ->
                    result.warnings().add("extra"))
                    .isInstanceOf(
                            UnsupportedOperationException
                                    .class);
        }

        @Test
        @DisplayName("modifying original files list"
                + " does not affect result")
        void create_whenCalled_originalFilesModificationSafe() {
            List<String> mutable = new ArrayList<>();
            mutable.add("file1");

            CicdResult result = new CicdResult(
                    mutable, List.of());

            mutable.add("file2");

            assertThat(result.files()).hasSize(1);
            assertThat(result.files())
                    .containsExactly("file1");
        }

        @Test
        @DisplayName("modifying original warnings list"
                + " does not affect result")
        void create_whenCalled_originalWarningsModificationSafe() {
            List<String> mutable = new ArrayList<>();
            mutable.add("warn1");

            CicdResult result = new CicdResult(
                    List.of(), mutable);

            mutable.add("warn2");

            assertThat(result.warnings()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("empty factory")
    class EmptyFactory {

        @Test
        @DisplayName("returns empty files and warnings")
        void empty_emptyResult_succeeds() {
            CicdResult result = CicdResult.empty();

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("merge")
    class Merge {

        @Test
        @DisplayName("merges multiple results")
        void create_whenCalled_mergesResults() {
            CicdResult r1 = new CicdResult(
                    List.of("f1"), List.of("w1"));
            CicdResult r2 = new CicdResult(
                    List.of("f2", "f3"), List.of());
            CicdResult r3 = new CicdResult(
                    List.of(), List.of("w2"));

            CicdResult merged =
                    CicdResult.merge(List.of(r1, r2, r3));

            assertThat(merged.files())
                    .containsExactly("f1", "f2", "f3");
            assertThat(merged.warnings())
                    .containsExactly("w1", "w2");
        }

        @Test
        @DisplayName("merging empty list returns empty")
        void create_whenCalled_mergesEmpty() {
            CicdResult merged =
                    CicdResult.merge(List.of());

            assertThat(merged.files()).isEmpty();
            assertThat(merged.warnings()).isEmpty();
        }
    }
}
