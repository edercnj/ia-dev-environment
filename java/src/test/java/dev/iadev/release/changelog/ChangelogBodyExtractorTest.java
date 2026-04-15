package dev.iadev.release.changelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Story-0039-0006 TDD — ChangelogBodyExtractor is a pure-domain helper that
 * extracts the body of a specific {@code ## [X.Y.Z]} section from a
 * Keep-a-Changelog-formatted document. TPP order: degenerate -> constant ->
 * scalar -> collection -> edge cases.
 */
class ChangelogBodyExtractorTest {

    @Nested
    class Degenerate {

        @Test
        void extract_nullChangelog_throwsNpe() {
            assertThatThrownBy(() -> ChangelogBodyExtractor.extract(null, "1.0.0"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("changelog");
        }

        @Test
        void extract_nullVersion_throwsNpe() {
            assertThatThrownBy(() -> ChangelogBodyExtractor.extract("", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("version");
        }

        @Test
        void extract_emptyChangelog_returnsEmpty() {
            Optional<String> result = ChangelogBodyExtractor.extract("", "1.0.0");
            assertThat(result).isEmpty();
        }

        @Test
        void extract_invalidSemver_throwsIae() {
            assertThatThrownBy(() -> ChangelogBodyExtractor.extract("## [1.0.0]\n\nBody", "not-a-version"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SemVer");
        }

        @Test
        void extract_semverWithSpecialRegexChars_isRejected() {
            // Defense against ReDoS / injection — SemVer pattern must not accept dots/brackets in position of meta.
            assertThatThrownBy(() -> ChangelogBodyExtractor.extract("## [1.0.0]\n\nBody", "1.0.0$"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class HappyPath {

        @Test
        void extract_versionPresent_returnsBodyBetweenHeaders() {
            String changelog = """
                    # Changelog

                    ## [Unreleased]

                    ## [3.2.0] - 2026-04-15

                    ### Added
                    - Feature A
                    - Feature B

                    ### Fixed
                    - Bug C

                    ## [3.1.0] - 2026-03-01

                    ### Added
                    - Older feature
                    """;

            Optional<String> result = ChangelogBodyExtractor.extract(changelog, "3.2.0");

            assertThat(result).isPresent();
            assertThat(result.get())
                    .contains("### Added")
                    .contains("Feature A")
                    .contains("Feature B")
                    .contains("### Fixed")
                    .contains("Bug C")
                    .doesNotContain("Older feature")
                    .doesNotContain("## [3.1.0]")
                    .doesNotContain("## [3.2.0]");
        }

        @Test
        void extract_lastVersionAtEof_returnsBodyUpToEnd() {
            String changelog = """
                    ## [1.0.0] - 2026-04-15

                    ### Added
                    - Initial
                    """;

            Optional<String> result = ChangelogBodyExtractor.extract(changelog, "1.0.0");

            assertThat(result).isPresent();
            assertThat(result.get()).contains("Initial");
        }
    }

    @Nested
    class Scalar {

        @Test
        void extract_versionMissing_returnsEmpty() {
            String changelog = """
                    ## [2.0.0]

                    body
                    """;

            Optional<String> result = ChangelogBodyExtractor.extract(changelog, "9.9.9");

            assertThat(result).isEmpty();
        }

        @Test
        void extract_multilineBodyPreserved_returnsExactContent() {
            String changelog = "## [1.2.3]\n\nLine1\nLine2\nLine3\n\n## [1.0.0]\n\nOlder\n";

            Optional<String> result = ChangelogBodyExtractor.extract(changelog, "1.2.3");

            assertThat(result).isPresent();
            assertThat(result.get())
                    .contains("Line1")
                    .contains("Line2")
                    .contains("Line3")
                    .doesNotContain("Older");
        }

        @Test
        void extract_versionWithPreRelease_matches() {
            String changelog = "## [1.0.0-rc.1]\n\nRC body\n\n## [0.9.0]\n\nold\n";

            Optional<String> result = ChangelogBodyExtractor.extract(changelog, "1.0.0-rc.1");

            assertThat(result).isPresent();
            assertThat(result.get()).contains("RC body").doesNotContain("old");
        }
    }

    @Nested
    class Edge {

        @Test
        void extract_trimsTrailingWhitespace() {
            String changelog = "## [1.0.0]\n\nbody\n\n\n\n## [0.9.0]\n\nold\n";

            Optional<String> result = ChangelogBodyExtractor.extract(changelog, "1.0.0");

            assertThat(result).isPresent();
            assertThat(result.get()).endsWith("body");
        }

        @Test
        void extract_emptyBodyBetweenHeaders_returnsEmptyString() {
            String changelog = "## [1.0.0]\n\n## [0.9.0]\n\nolder\n";

            Optional<String> result = ChangelogBodyExtractor.extract(changelog, "1.0.0");

            assertThat(result).isPresent();
            assertThat(result.get()).isEmpty();
        }

        @Test
        void extract_bodyWithOnlyBlankLines_returnsEmpty() {
            String changelog = "## [1.0.0]\n\n   \n\t\n\n## [0.9.0]\nold\n";

            Optional<String> result = ChangelogBodyExtractor.extract(changelog, "1.0.0");

            assertThat(result).isPresent();
            assertThat(result.get()).isEmpty();
        }

        @Test
        void extract_bodyEndsWithTrailingNewlines_stripped() {
            // exercise the second while-loop in trimBlankLines (end > start && '\n')
            String changelog = "## [1.0.0]\n\nbody\n\n";

            Optional<String> result = ChangelogBodyExtractor.extract(changelog, "1.0.0");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("body");
        }

        @Test
        void extract_versionWithBuildMetadata_matches() {
            String changelog = "## [1.0.0+build.123]\n\nbuild body\n";

            Optional<String> result = ChangelogBodyExtractor.extract(changelog, "1.0.0+build.123");

            assertThat(result).isPresent();
            assertThat(result.get()).contains("build body");
        }

        @Test
        void extract_bodyEndsWithWhitespaceOnlyLine_stripsTrailingBlanks() {
            // The extracted section ends with "body\n  \n\t\n" — trailing
            // whitespace-only lines must be trimmed per trimBlankLines contract.
            String changelog = "## [1.0.0]\n\nbody\n  \n\t\n## [0.9.0]\n\nold\n";

            Optional<String> result = ChangelogBodyExtractor.extract(changelog, "1.0.0");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("body");
        }

        @Test
        void extract_bodyEndsWithWhitespaceOnlyLineAtEof_stripsTrailingBlanks() {
            // Last-section variant: whitespace-only line preceding EOF.
            String changelog = "## [1.0.0]\n\nbody\n   \n\t\n";

            Optional<String> result = ChangelogBodyExtractor.extract(changelog, "1.0.0");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("body");
        }
    }
}
