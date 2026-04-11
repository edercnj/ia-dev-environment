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
        void singlePlatform_whenCalled_showsPlatformCounts() {
            List<AssemblerDescriptor> all = allAssemblers();
            List<AssemblerDescriptor> filtered = List.of(
                    claudeDesc("RulesAssembler"),
                    sharedDesc("ConstitutionAssembler"));

            String header =
                    PlatformVerboseFormatter
                            .formatFilterHeader(
                                    Set.of(
                                            Platform
                                                    .CLAUDE_CODE),
                                    filtered, all);

            assertThat(header).isEqualTo(
                    "Platform filter: claude-code"
                    + " -> 2 assemblers"
                    + " (1 platform + 1 shared)");
        }

        @Test
        void singlePlatformWithShared_whenCalled_showsCombinedNames() {
            List<AssemblerDescriptor> all = allAssemblers();
            List<AssemblerDescriptor> filtered = List.of(
                    codexDesc("CodexConfigAssembler"),
                    sharedDesc("ConstitutionAssembler"));

            String header =
                    PlatformVerboseFormatter
                            .formatFilterHeader(
                                    Set.of(Platform.CODEX),
                                    filtered, all);

            assertThat(header).contains(
                    "Platform filter: codex");
            assertThat(header).contains(
                    "-> 2 assemblers");
            assertThat(header).contains(
                    "(1 platform + 1 shared)");
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
        void codexAssembler_whenCalled_showsCodexPlatform() {
            AssemblerDescriptor desc =
                    codexDesc("CodexConfigAssembler");

            String line =
                    PlatformVerboseFormatter
                            .formatSkipped(desc);

            assertThat(line).isEqualTo(
                    "  SKIPPED: CodexConfigAssembler"
                    + " (platform: codex)");
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
                                    Set.of(), 27);

            assertThat(warning).isEqualTo(
                    "Dry run -- no files written."
                    + " Platform: all (27 assemblers)");
        }

        @Test
        void singlePlatform_whenCalled_showsPlatformName() {
            String warning =
                    PlatformVerboseFormatter
                            .formatDryRunWarning(
                                    Set.of(
                                            Platform.CODEX),
                                    19);

            assertThat(warning).isEqualTo(
                    "Dry run -- no files written."
                    + " Platform: codex (19 assemblers)");
        }

        @Test
        void allPlatformsExplicit_whenCalled_showsAllLabel() {
            String warning =
                    PlatformVerboseFormatter
                            .formatDryRunWarning(
                                    Set.of(
                                            Platform.CLAUDE_CODE,
                                            Platform.CODEX),
                                    27);

            assertThat(warning).contains("Platform: all");
            assertThat(warning).contains("27 assemblers");
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
                    all.get(0), all.get(1));

            List<AssemblerDescriptor> skipped =
                    PlatformVerboseFormatter
                            .computeSkipped(filtered, all);

            assertThat(skipped).hasSize(all.size() - 2);
        }
    }

    // --- helpers ---

    private static List<AssemblerDescriptor> allAssemblers() {
        return List.of(
                claudeDesc("RulesAssembler"),
                codexDesc("CodexConfigAssembler"),
                sharedDesc("ConstitutionAssembler"));
    }

    private static AssemblerDescriptor claudeDesc(
            String name) {
        return new AssemblerDescriptor(
                name, AssemblerTarget.CLAUDE,
                Set.of(Platform.CLAUDE_CODE),
                (c, e, p) -> List.of());
    }

    private static AssemblerDescriptor codexDesc(
            String name) {
        return new AssemblerDescriptor(
                name, AssemblerTarget.CODEX,
                Set.of(Platform.CODEX),
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
