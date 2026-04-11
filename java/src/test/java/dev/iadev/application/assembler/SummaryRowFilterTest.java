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
            {"AGENTS.md (root)", 1},
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
        @DisplayName("claude-only treated as all"
                + " (single user-selectable)")
        void filter_claudeOnly_treatedAsAll() {
            Object[][] result = SummaryRowFilter.filter(
                    ALL_ROWS,
                    Set.of(Platform.CLAUDE_CODE));

            // Post-Codex-removal: CLAUDE_CODE is the only
            // user-selectable platform, so shouldShowAll
            // returns true and the filter is bypassed.
            assertThat(result.length)
                    .isEqualTo(ALL_ROWS.length);
        }
    }
}
