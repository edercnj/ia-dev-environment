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
        void multiplePlatforms_whenCalled_showsCombinedNames() {
            List<AssemblerDescriptor> all = allAssemblers();
            List<AssemblerDescriptor> filtered = List.of(
                    claudeDesc("RulesAssembler"),
                    copilotDesc("GithubSkillsAssembler"),
                    sharedDesc("ConstitutionAssembler"));

            String header =
                    PlatformVerboseFormatter
                            .formatFilterHeader(
                                    Set.of(
                                            Platform.CLAUDE_CODE,
                                            Platform.COPILOT),
                                    filtered, all);

            assertThat(header).contains(
                    "Platform filter: claude-code, copilot");
            assertThat(header).contains(
                    "-> 3 assemblers");
            assertThat(header).contains(
                    "(2 platform + 1 shared)");
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
        void copilotAssembler_whenCalled_showsCopilotPlatform() {
            AssemblerDescriptor desc =
                    copilotDesc("GithubSkillsAssembler");

            String line =
                    PlatformVerboseFormatter
                            .formatSkipped(desc);

            assertThat(line).isEqualTo(
                    "  SKIPPED: GithubSkillsAssembler"
                    + " (platform: copilot)");
        }

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
                                    Set.of(), 34);

            assertThat(warning).isEqualTo(
                    "Dry run -- no files written."
                    + " Platform: all (34 assemblers)");
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
        void multiplePlatforms_whenCalled_showsJoinedNames() {
            String warning =
                    PlatformVerboseFormatter
                            .formatDryRunWarning(
                                    Set.of(
                                            Platform.CLAUDE_CODE,
                                            Platform.COPILOT),
                                    29);

            assertThat(warning).contains("Platform:");
            assertThat(warning).contains("claude-code");
            assertThat(warning).contains("copilot");
            assertThat(warning).contains("29 assemblers");
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
                copilotDesc("GithubSkillsAssembler"),
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

    private static AssemblerDescriptor copilotDesc(
            String name) {
        return new AssemblerDescriptor(
                name, AssemblerTarget.GITHUB,
                Set.of(Platform.COPILOT),
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
