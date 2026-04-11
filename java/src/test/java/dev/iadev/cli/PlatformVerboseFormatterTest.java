package dev.iadev.cli;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.AssemblerTarget;
import dev.iadev.domain.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PlatformVerboseFormatter}.
 *
 * <p>TPP ordering: constant -> scalar -> collection.
 */
@DisplayName("PlatformVerboseFormatter")
class PlatformVerboseFormatterTest {

    @Nested
    @DisplayName("formatFilterHeader")
    class FormatFilterHeader {

        @Test
        void noFilter_whenCalled_showsNoFilterApplied() {
            List<AssemblerDescriptor> all = allAssemblers();

            String header =
                    PlatformVerboseFormatter
                            .formatFilterHeader(
                                    Set.of(), all, all);

            assertThat(header).isEqualTo(
                    "Platform filter: all -> %d assemblers"
                            .formatted(all.size())
                    + " (no filter applied)");
        }

        @Test
        void singlePlatformAll_whenCalled_showsAllLabel() {
            List<AssemblerDescriptor> all = allAssemblers();
            List<AssemblerDescriptor> filtered = List.of(
                    claudeDesc("RulesAssembler"),
                    sharedDesc("ConstitutionAssembler"));

            // Post-Codex-removal: CLAUDE_CODE is the only
            // user-selectable platform, so specifying it is
            // semantically equivalent to "all" (no filter).
            String header =
                    PlatformVerboseFormatter
                            .formatFilterHeader(
                                    Set.of(
                                            Platform
                                                    .CLAUDE_CODE),
                                    filtered, all);

            assertThat(header).isEqualTo(
                    "Platform filter: all"
                    + " -> %d assemblers"
                            .formatted(all.size())
                    + " (no filter applied)");
        }
    }

    @Nested
    @DisplayName("formatIncluded")
    class FormatIncluded {

        @Test
        void platformAssembler_whenCalled_showsPlatformName() {
            AssemblerDescriptor desc =
                    claudeDesc("RulesAssembler");

            String line =
                    PlatformVerboseFormatter
                            .formatIncluded(desc);

            assertThat(line).isEqualTo(
                    "  INCLUDED: RulesAssembler"
                    + " (platform: claude-code)");
        }

        @Test
        void sharedAssembler_whenCalled_showsSharedPlatform() {
            AssemblerDescriptor desc =
                    sharedDesc("DocsAssembler");

            String line =
                    PlatformVerboseFormatter
                            .formatIncluded(desc);

            assertThat(line).isEqualTo(
                    "  INCLUDED: DocsAssembler"
                    + " (platform: shared)");
        }
    }

    @Nested
    @DisplayName("formatSkipped")
    class FormatSkipped {

        @Test
        void claudeAssembler_whenCalled_showsClaudePlatform() {
            AssemblerDescriptor desc =
                    claudeDesc("RulesAssembler");

            String line =
                    PlatformVerboseFormatter
                            .formatSkipped(desc);

            assertThat(line).isEqualTo(
                    "  SKIPPED: RulesAssembler"
                    + " (platform: claude-code)");
        }
    }

    @Nested
    @DisplayName("formatDryRunWarning")
    class FormatDryRunWarning {

        @Test
        void noFilter_whenCalled_showsAllPlatform() {
            String warning =
                    PlatformVerboseFormatter
                            .formatDryRunWarning(
                                    Set.of(), 22);

            assertThat(warning).isEqualTo(
                    "Dry run -- no files written."
                    + " Platform: all (22 assemblers)");
        }

        @Test
        void singlePlatform_whenCalled_showsAllLabel() {
            String warning =
                    PlatformVerboseFormatter
                            .formatDryRunWarning(
                                    Set.of(
                                            Platform
                                                    .CLAUDE_CODE),
                                    22);

            // Post-Codex-removal: specifying the only
            // user-selectable platform is equivalent to
            // "all" (no filter).
            assertThat(warning).isEqualTo(
                    "Dry run -- no files written."
                    + " Platform: all"
                    + " (22 assemblers)");
        }
    }

    @Nested
    @DisplayName("computeSkipped")
    class ComputeSkipped {

        @Test
        void noFilter_whenCalled_returnsEmptyList() {
            List<AssemblerDescriptor> all = allAssemblers();

            List<AssemblerDescriptor> skipped =
                    PlatformVerboseFormatter
                            .computeSkipped(all, all);

            assertThat(skipped).isEmpty();
        }

        @Test
        void withFilter_whenCalled_returnsExcludedItems() {
            List<AssemblerDescriptor> all = allAssemblers();
            List<AssemblerDescriptor> filtered = List.of(
                    all.get(0));

            List<AssemblerDescriptor> skipped =
                    PlatformVerboseFormatter
                            .computeSkipped(filtered, all);

            assertThat(skipped).hasSize(all.size() - 1);
        }
    }

    // --- helpers ---

    private static List<AssemblerDescriptor> allAssemblers() {
        return List.of(
                claudeDesc("RulesAssembler"),
                sharedDesc("ConstitutionAssembler"));
    }

    private static AssemblerDescriptor claudeDesc(
            String name) {
        return new AssemblerDescriptor(
                name, AssemblerTarget.CLAUDE,
                Set.of(Platform.CLAUDE_CODE),
                (c, e, p) -> List.of());
    }

    private static AssemblerDescriptor sharedDesc(
            String name) {
        return new AssemblerDescriptor(
                name, AssemblerTarget.ROOT,
                Set.of(Platform.SHARED),
                (c, e, p) -> List.of());
    }
}
