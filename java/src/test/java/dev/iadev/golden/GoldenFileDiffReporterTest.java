package dev.iadev.golden;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GoldenFileDiffReporter}.
 *
 * <p>Verifies diff generation for identical content,
 * differing content, whitespace differences, and output
 * line limiting.</p>
 */
@DisplayName("GoldenFileDiffReporter")
class GoldenFileDiffReporterTest {

    @Nested
    @DisplayName("identical content")
    class IdenticalContent {

        @Test
        void identicalStrings_whenCalled_returnsEmpty() {
            String content = "line1\nline2\nline3\n";

            String diff = GoldenFileDiffReporter.generateDiff(
                    "test.md", content, content);

            assertThat(diff).isEmpty();
        }

        @Test
        void emptyStrings_whenCalled_returnsEmpty() {
            String diff = GoldenFileDiffReporter.generateDiff(
                    "test.md", "", "");

            assertThat(diff).isEmpty();
        }
    }

    @Nested
    @DisplayName("differing content")
    class DifferingContent {

        @Test
        void singleLineDifference_whenCalled_showsContext() {
            String expected = "line1\nline2\nline3\n";
            String actual = "line1\nmodified\nline3\n";

            String diff = GoldenFileDiffReporter.generateDiff(
                    "test.md", expected, actual);

            assertThat(diff)
                    .contains("test.md")
                    .contains("line2")
                    .contains("modified");
        }

        @Test
        void addedLine_whenCalled_showsAddition() {
            String expected = "line1\nline2\n";
            String actual = "line1\nline2\nline3\n";

            String diff = GoldenFileDiffReporter.generateDiff(
                    "test.md", expected, actual);

            assertThat(diff)
                    .contains("test.md")
                    .contains("line3");
        }

        @Test
        void removedLine_whenCalled_showsRemoval() {
            String expected = "line1\nline2\nline3\n";
            String actual = "line1\nline3\n";

            String diff = GoldenFileDiffReporter.generateDiff(
                    "test.md", expected, actual);

            assertThat(diff)
                    .contains("test.md")
                    .contains("line2");
        }
    }

    @Nested
    @DisplayName("whitespace differences")
    class WhitespaceDifferences {

        @Test
        void trailingSpaces_whenCalled_detected() {
            String expected = "line1\nline2\n";
            String actual = "line1\nline2 \n";

            String diff = GoldenFileDiffReporter.generateDiff(
                    "test.md", expected, actual);

            assertThat(diff).isNotEmpty();
        }

        @Test
        void crlfVsLf_whenCalled_detected() {
            String expected = "line1\nline2\n";
            String actual = "line1\r\nline2\r\n";

            String diff = GoldenFileDiffReporter.generateDiff(
                    "test.md", expected, actual);

            assertThat(diff)
                    .isNotEmpty()
                    .contains("\\r\\n");
        }
    }

    @Nested
    @DisplayName("output limiting")
    class OutputLimiting {

        @Test
        void longDiff_whenCalled_truncatedToMaxLines() {
            StringBuilder expected = new StringBuilder();
            StringBuilder actual = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                expected.append("expected-line-")
                        .append(i).append("\n");
                actual.append("actual-line-")
                        .append(i).append("\n");
            }

            String diff = GoldenFileDiffReporter.generateDiff(
                    "test.md",
                    expected.toString(),
                    actual.toString());

            assertThat(diff)
                    .contains("truncated");

            long lineCount = diff.lines().count();
            // MAX_DIFF_LINES + 1 truncation message
            assertThat(lineCount)
                    .isLessThanOrEqualTo(
                            GoldenFileDiffReporter
                                    .MAX_DIFF_LINES + 1);
        }
    }
}
