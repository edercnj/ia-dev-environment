package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SummaryRowFilter — filters generation summary
 * rows by platform.
 */
@DisplayName("SummaryRowFilter")
class SummaryRowFilterTest {

    private static final Object[][] ALL_ROWS = {
            {"Rules (.claude)", 6},
            {"Skills (.claude)", 14},
            {"Instructions (.github)", 5},
            {"Skills (.github)", 32},
            {"AGENTS.md (root)", 1},
            {"Codex (.codex)", 2},
            {"Skills (.agents)", 3},
    };

    @Nested
    @DisplayName("filter")
    class Filter {

        @Test
        @DisplayName("empty platforms returns all rows")
        void filter_emptyPlatforms_returnsAllRows() {
            Object[][] result = SummaryRowFilter.filter(
                    ALL_ROWS, Set.of());

            assertThat(result.length)
                    .isEqualTo(ALL_ROWS.length);
        }

        @Test
        @DisplayName("all selectable returns all rows")
        void filter_allSelectable_returnsAllRows() {
            Object[][] result = SummaryRowFilter.filter(
                    ALL_ROWS,
                    Platform.allUserSelectable());

            assertThat(result.length)
                    .isEqualTo(ALL_ROWS.length);
        }

        @Test
        @DisplayName("claude-only keeps only .claude rows")
        void filter_claudeOnly_keepsClaudeRows() {
            Object[][] result = SummaryRowFilter.filter(
                    ALL_ROWS,
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(result.length).isEqualTo(2);
            assertThat((String) result[0][0])
                    .contains("(.claude)");
            assertThat((String) result[1][0])
                    .contains("(.claude)");
        }

        @Test
        @DisplayName("copilot-only keeps only .github rows")
        void filter_copilotOnly_keepsGithubRows() {
            Object[][] result = SummaryRowFilter.filter(
                    ALL_ROWS,
                    Set.of(Platform.COPILOT));

            assertThat(result.length).isEqualTo(2);
            assertThat((String) result[0][0])
                    .contains("(.github)");
        }

        @Test
        @DisplayName("codex-only keeps codex rows")
        void filter_codexOnly_keepsCodexRows() {
            Object[][] result = SummaryRowFilter.filter(
                    ALL_ROWS,
                    Set.of(Platform.CODEX));

            assertThat(result.length).isEqualTo(3);
            assertThat((String) result[0][0])
                    .contains("AGENTS.md");
            assertThat((String) result[1][0])
                    .contains("(.codex)");
            assertThat((String) result[2][0])
                    .contains("(.agents)");
        }
    }
}
