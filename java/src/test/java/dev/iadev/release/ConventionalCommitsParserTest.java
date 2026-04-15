package dev.iadev.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ConventionalCommitsParser — classify commit payloads into CommitCounts")
class ConventionalCommitsParserTest {

    @Nested
    @DisplayName("Degenerate inputs (TPP Level 1 — nil)")
    class Degenerate {

        @Test
        @DisplayName("classify_emptyCommitList_returnsZeroCounts")
        void classify_emptyCommitList_returnsZeroCounts() {
            CommitCounts result = ConventionalCommitsParser.classify(List.of());

            assertThat(result).isEqualTo(CommitCounts.ZERO);
            assertThat(result.feat()).isZero();
            assertThat(result.fix()).isZero();
            assertThat(result.perf()).isZero();
            assertThat(result.breaking()).isZero();
            assertThat(result.ignored()).isZero();
        }
    }

    @Nested
    @DisplayName("Constant inputs (TPP Level 2 — single commit)")
    class Constant {

        @Test
        @DisplayName("classify_singleFeatCommit_incrementsFeat")
        void classify_singleFeatCommit_incrementsFeat() {
            CommitCounts result = ConventionalCommitsParser.classify(
                    List.of("feat: add login flow"));

            assertThat(result.feat()).isEqualTo(1);
            assertThat(result.fix()).isZero();
            assertThat(result.perf()).isZero();
            assertThat(result.breaking()).isZero();
            assertThat(result.ignored()).isZero();
        }

        @Test
        @DisplayName("classify_singleDocsCommit_incrementsIgnored")
        void classify_singleDocsCommit_incrementsIgnored() {
            CommitCounts result = ConventionalCommitsParser.classify(
                    List.of("docs: update README"));

            assertThat(result.ignored()).isEqualTo(1);
            assertThat(result.feat()).isZero();
        }

        @Test
        @DisplayName("classify_featWithScope_incrementsFeat")
        void classify_featWithScope_incrementsFeat() {
            CommitCounts result = ConventionalCommitsParser.classify(
                    List.of("feat(auth): support OAuth"));

            assertThat(result.feat()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Scalar inputs (TPP Level 3 — mixed commits)")
    class Scalar {

        @Test
        @DisplayName("classify_mixedFeatFix_countsBothCorrectly")
        void classify_mixedFeatFix_countsBothCorrectly() {
            CommitCounts result = ConventionalCommitsParser.classify(List.of(
                    "feat: a",
                    "feat: b",
                    "fix: c",
                    "docs: d"));

            assertThat(result.feat()).isEqualTo(2);
            assertThat(result.fix()).isEqualTo(1);
            assertThat(result.ignored()).isEqualTo(1);
        }

        @Test
        @DisplayName("classify_perfCommit_incrementsPerf")
        void classify_perfCommit_incrementsPerf() {
            CommitCounts result = ConventionalCommitsParser.classify(
                    List.of("perf: cache tokens"));

            assertThat(result.perf()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Conditional inputs (TPP Level 4 — breaking-change markers)")
    class Conditional {

        @Test
        @DisplayName("classify_featBangNotation_incrementsBreaking")
        void classify_featBangNotation_incrementsBreaking() {
            CommitCounts result = ConventionalCommitsParser.classify(
                    List.of("feat!: drop legacy API"));

            assertThat(result.feat()).isEqualTo(1);
            assertThat(result.breaking()).isEqualTo(1);
        }

        @Test
        @DisplayName("classify_fixBang_incrementsBreaking")
        void classify_fixBang_incrementsBreaking() {
            CommitCounts result = ConventionalCommitsParser.classify(
                    List.of("fix!: change default port"));

            assertThat(result.fix()).isEqualTo(1);
            assertThat(result.breaking()).isEqualTo(1);
        }

        @Test
        @DisplayName("classify_bodyBreakingChange_incrementsBreaking")
        void classify_bodyBreakingChange_incrementsBreaking() {
            String payload = "feat: rework pricing\n\n"
                    + "Body line one.\n"
                    + "BREAKING CHANGE: old pricing endpoints removed.\n";

            CommitCounts result = ConventionalCommitsParser.classify(List.of(payload));

            assertThat(result.feat()).isEqualTo(1);
            assertThat(result.breaking()).isEqualTo(1);
        }

        @Test
        @DisplayName("classify_bangAndBreakingChangeFooter_countsBreakingOnceNotTwice")
        void classify_bangAndBreakingChangeFooter_countsBreakingOnceNotTwice() {
            // A single commit that uses BOTH the bang notation AND a
            // BREAKING CHANGE: footer must be counted as exactly one
            // breaking commit — not two. Double-counting would inflate
            // banner metrics (PR #374 review comment).
            String payload = "feat!: rework pricing\n\n"
                    + "Body line one.\n"
                    + "BREAKING CHANGE: old pricing endpoints removed.\n";

            CommitCounts result = ConventionalCommitsParser.classify(List.of(payload));

            assertThat(result.feat()).isEqualTo(1);
            assertThat(result.breaking()).isEqualTo(1);
        }

        @Test
        @DisplayName("classify_unknownSubject_incrementsIgnored")
        void classify_unknownSubject_incrementsIgnored() {
            CommitCounts result = ConventionalCommitsParser.classify(
                    List.of("no convention here"));

            assertThat(result.ignored()).isEqualTo(1);
        }

        @Test
        @DisplayName("classify_nullPayload_treatedAsIgnored")
        void classify_nullPayload_treatedAsIgnored() {
            CommitCounts result = ConventionalCommitsParser.classify(
                    java.util.Arrays.asList("feat: ok", null, ""));

            assertThat(result.feat()).isEqualTo(1);
            assertThat(result.ignored()).isEqualTo(2);
        }
    }
}
