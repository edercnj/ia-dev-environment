package dev.iadev.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the shared AssemblerResult record --
 * immutable result of an assembler operation.
 */
@DisplayName("AssemblerResult")
class AssemblerResultTest {

    @Nested
    @DisplayName("compact constructor -- null safety")
    class NullSafety {

        @Test
        @DisplayName("nullFiles_nullWarnings_"
                + "producesEmptyLists")
        void nullFiles_nullWarnings_producesEmptyLists() {
            AssemblerResult result =
                    new AssemblerResult(null, null);

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("nullFiles_nonNullWarnings_"
                + "producesEmptyFiles")
        void nullFiles_nonNullWarnings_producesEmptyFiles() {
            AssemblerResult result =
                    new AssemblerResult(
                            null, List.of("warn1"));

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings())
                    .containsExactly("warn1");
        }

        @Test
        @DisplayName("nonNullFiles_nullWarnings_"
                + "producesEmptyWarnings")
        void nonNullFiles_nullWarnings_producesEmptyWarnings() {
            AssemblerResult result =
                    new AssemblerResult(
                            List.of("f1"), null);

            assertThat(result.files())
                    .containsExactly("f1");
            assertThat(result.warnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("immutability")
    class Immutability {

        @Test
        @DisplayName("files_returnsImmutableList")
        void files_whenCalled_returnsImmutableList() {
            AssemblerResult result =
                    new AssemblerResult(
                            List.of("a.md", "b.md"),
                            List.of());

            assertThatThrownBy(() ->
                    result.files().add("c.md"))
                    .isInstanceOf(
                            UnsupportedOperationException
                                    .class);
        }

        @Test
        @DisplayName("warnings_returnsImmutableList")
        void warnings_whenCalled_returnsImmutableList() {
            AssemblerResult result =
                    new AssemblerResult(
                            List.of(),
                            List.of("warn1"));

            assertThatThrownBy(() ->
                    result.warnings().add("warn2"))
                    .isInstanceOf(
                            UnsupportedOperationException
                                    .class);
        }

        @Test
        @DisplayName("mutableInputList_"
                + "doesNotAffectResult")
        void mutableInputList_whenCalled_doesNotAffectResult() {
            List<String> mutableFiles =
                    new ArrayList<>(List.of("x.md"));
            List<String> mutableWarnings =
                    new ArrayList<>(List.of("w1"));

            AssemblerResult result =
                    new AssemblerResult(
                            mutableFiles, mutableWarnings);

            mutableFiles.add("y.md");
            mutableWarnings.add("w2");

            assertThat(result.files())
                    .containsExactly("x.md");
            assertThat(result.warnings())
                    .containsExactly("w1");
        }
    }

    @Nested
    @DisplayName("factory method -- empty")
    class EmptyFactory {

        @Test
        @DisplayName("empty_returnsEmptyFilesAndWarnings")
        void empty_whenCalled_returnsEmptyFilesAndWarnings() {
            AssemblerResult result =
                    AssemblerResult.empty();

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("empty_filesAreImmutable")
        void empty_whenCalled_filesAreImmutable() {
            AssemblerResult result =
                    AssemblerResult.empty();

            assertThatThrownBy(() ->
                    result.files().add("x"))
                    .isInstanceOf(
                            UnsupportedOperationException
                                    .class);
        }
    }

    @Nested
    @DisplayName("factory method -- of")
    class OfFactory {

        @Test
        @DisplayName("of_preservesFilesAndWarnings")
        void of_whenCalled_preservesFilesAndWarnings() {
            AssemblerResult result =
                    AssemblerResult.of(
                            List.of("f1", "f2"),
                            List.of("w1"));

            assertThat(result.files())
                    .containsExactly("f1", "f2");
            assertThat(result.warnings())
                    .containsExactly("w1");
        }

        @Test
        @DisplayName("of_nullArgs_producesEmptyLists")
        void of_nullArgs_producesEmptyLists() {
            AssemblerResult result =
                    AssemblerResult.of(null, null);

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("record contract -- equals, hashCode,"
            + " toString")
    class RecordContract {

        @Test
        @DisplayName("equals_sameValues_returnsTrue")
        void equals_sameValues_returnsTrue() {
            AssemblerResult a = AssemblerResult.of(
                    List.of("f1"), List.of("w1"));
            AssemblerResult b = AssemblerResult.of(
                    List.of("f1"), List.of("w1"));

            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("equals_differentValues_"
                + "returnsFalse")
        void equals_differentValues_returnsFalse() {
            AssemblerResult a = AssemblerResult.of(
                    List.of("f1"), List.of());
            AssemblerResult b = AssemblerResult.of(
                    List.of("f2"), List.of());

            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("hashCode_sameValues_sameHash")
        void hashCode_sameValues_sameHash() {
            AssemblerResult a = AssemblerResult.of(
                    List.of("f1"), List.of("w1"));
            AssemblerResult b = AssemblerResult.of(
                    List.of("f1"), List.of("w1"));

            assertThat(a.hashCode())
                    .isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("toString_containsFieldValues")
        void toString_whenCalled_containsFieldValues() {
            AssemblerResult result = AssemblerResult.of(
                    List.of("file.md"), List.of("warn"));

            String str = result.toString();

            assertThat(str)
                    .contains("file.md")
                    .contains("warn");
        }
    }
}
