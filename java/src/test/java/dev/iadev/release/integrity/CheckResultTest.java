package dev.iadev.release.integrity;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke tests for {@link CheckResult} factories, validation,
 * and files-list immutability (audit finding M-017).
 */
@DisplayName("CheckResultTest")
class CheckResultTest {

    @Nested
    @DisplayName("factories — status routing")
    class Factories {

        @Test
        @DisplayName("pass_statusPass_emptyFiles")
        void pass_statusPass_emptyFiles() {
            CheckResult result = CheckResult.pass("todo_scan");

            assertThat(result.name()).isEqualTo("todo_scan");
            assertThat(result.status())
                    .isEqualTo(CheckStatus.PASS);
            assertThat(result.files()).isEmpty();
        }

        @Test
        @DisplayName("fail_statusFail_filesPreserved")
        void fail_statusFail_filesPreserved() {
            CheckResult result = CheckResult.fail(
                    "version_drift",
                    List.of("pom.xml:42", "CHANGELOG.md:7"));

            assertThat(result.name())
                    .isEqualTo("version_drift");
            assertThat(result.status())
                    .isEqualTo(CheckStatus.FAIL);
            assertThat(result.files()).containsExactly(
                    "pom.xml:42", "CHANGELOG.md:7");
        }

        @Test
        @DisplayName("warn_statusWarn_filesPreserved")
        void warn_statusWarn_filesPreserved() {
            CheckResult result = CheckResult.warn(
                    "todo_scan", List.of("src/Foo.java:10"));

            assertThat(result.status())
                    .isEqualTo(CheckStatus.WARN);
            assertThat(result.files()).containsExactly(
                    "src/Foo.java:10");
        }
    }

    @Nested
    @DisplayName("constructor — validation and defensive copy")
    class Constructor {

        @Test
        @DisplayName("constructor_nullName_throws")
        void constructor_nullName_throws() {
            assertThatThrownBy(() -> new CheckResult(
                    null, CheckStatus.PASS, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("constructor_nullStatus_throws")
        void constructor_nullStatus_throws() {
            assertThatThrownBy(() -> new CheckResult(
                    "x", null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("status");
        }

        @Test
        @DisplayName("constructor_nullFiles_throws")
        void constructor_nullFiles_throws() {
            assertThatThrownBy(() -> new CheckResult(
                    "x", CheckStatus.PASS, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("files");
        }

        @Test
        @DisplayName("files_defensiveCopy_isImmutable")
        void files_defensiveCopy_isImmutable() {
            List<String> mutable = new ArrayList<>();
            mutable.add("pom.xml");

            CheckResult result = CheckResult.fail(
                    "version_drift", mutable);

            // Mutation to source does not leak into record.
            mutable.add("CHANGELOG.md");
            assertThat(result.files())
                    .containsExactly("pom.xml");
            // Returned list is itself unmodifiable.
            assertThatThrownBy(() ->
                    result.files().add("extra"))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }
    }
}
