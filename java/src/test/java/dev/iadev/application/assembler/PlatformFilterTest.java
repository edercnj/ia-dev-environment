package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PlatformFilter — filters assembler descriptors
 * by target platform(s).
 */
@DisplayName("PlatformFilter")
class PlatformFilterTest {

    @Nested
    @DisplayName("no filter scenarios")
    class NoFilter {

        @Test
        @DisplayName("empty platforms returns all "
                + "descriptors unchanged")
        void filter_emptyPlatforms_returnsAll() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(
                            all, Set.of());

            assertThat(result).hasSize(34);
            assertThat(result).isEqualTo(all);
        }

        @Test
        @DisplayName("all user-selectable platforms "
                + "returns all descriptors")
        void filter_allPlatforms_returnsAll() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.CLAUDE_CODE,
                                    Platform.COPILOT,
                                    Platform.CODEX));

            assertThat(result).hasSize(34);
            assertThat(result).isEqualTo(all);
        }
    }

    @Nested
    @DisplayName("single platform filter")
    class SinglePlatform {

        @Test
        @DisplayName("CLAUDE_CODE returns 22 assemblers "
                + "(8 claude + 14 shared)")
        void filter_claudeCode_returns22() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.CLAUDE_CODE));

            assertThat(result).hasSize(22);
            assertThat(result).allSatisfy(d ->
                    assertThat(
                            d.platforms().contains(
                                    Platform.CLAUDE_CODE)
                            || d.platforms().contains(
                                    Platform.SHARED))
                            .isTrue());
        }

        @Test
        @DisplayName("COPILOT returns 21 assemblers "
                + "(7 copilot + 14 shared)")
        void filter_copilot_returns21() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.COPILOT));

            assertThat(result).hasSize(21);
            assertThat(result).allSatisfy(d ->
                    assertThat(
                            d.platforms().contains(
                                    Platform.COPILOT)
                            || d.platforms().contains(
                                    Platform.SHARED))
                            .isTrue());
        }

        @Test
        @DisplayName("CODEX returns 19 assemblers "
                + "(5 codex + 14 shared)")
        void filter_codex_returns19() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.CODEX));

            assertThat(result).hasSize(19);
            assertThat(result).allSatisfy(d ->
                    assertThat(
                            d.platforms().contains(
                                    Platform.CODEX)
                            || d.platforms().contains(
                                    Platform.SHARED))
                            .isTrue());
        }

        @Test
        @DisplayName("CLAUDE_CODE excludes COPILOT and "
                + "CODEX exclusive assemblers")
        void filter_claudeCode_excludesCopilotAndCodex() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.CLAUDE_CODE));

            assertThat(result).noneMatch(d ->
                    d.platforms().contains(
                            Platform.COPILOT)
                    && !d.platforms().contains(
                            Platform.SHARED));
            assertThat(result).noneMatch(d ->
                    d.platforms().contains(Platform.CODEX)
                    && !d.platforms().contains(
                            Platform.SHARED));
        }
    }

    @Nested
    @DisplayName("multi-platform filter")
    class MultiPlatform {

        @Test
        @DisplayName("CLAUDE_CODE + COPILOT returns 29 "
                + "assemblers")
        void filter_claudeAndCopilot_returns29() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.CLAUDE_CODE,
                                    Platform.COPILOT));

            assertThat(result).hasSize(29);
        }

        @Test
        @DisplayName("CLAUDE_CODE + CODEX returns 27 "
                + "assemblers")
        void filter_claudeAndCodex_returns27() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.CLAUDE_CODE,
                                    Platform.CODEX));

            assertThat(result).hasSize(27);
        }

        @Test
        @DisplayName("COPILOT + CODEX returns 26 "
                + "assemblers")
        void filter_copilotAndCodex_returns26() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.COPILOT,
                                    Platform.CODEX));

            assertThat(result).hasSize(26);
        }
    }

    @Nested
    @DisplayName("SHARED always included (RULE-003)")
    class SharedAlwaysIncluded {

        @Test
        @DisplayName("CLAUDE_CODE includes all 14 "
                + "SHARED assemblers")
        void filter_claudeCode_includes14Shared() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.CLAUDE_CODE));

            long sharedCount = result.stream()
                    .filter(d -> d.platforms()
                            .contains(Platform.SHARED))
                    .count();
            assertThat(sharedCount).isEqualTo(14);
        }

        @Test
        @DisplayName("ConstitutionAssembler always "
                + "present when filtering")
        void filter_anyPlatform_constitutionPresent() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            for (Platform p :
                    Platform.allUserSelectable()) {
                List<AssemblerDescriptor> result =
                        PlatformFilter.filter(
                                all, Set.of(p));
                assertThat(result)
                        .as("ConstitutionAssembler must "
                                + "be present for %s", p)
                        .anyMatch(d ->
                                "ConstitutionAssembler"
                                        .equals(d.name()));
            }
        }
    }

    @Nested
    @DisplayName("order preservation (RULE-002)")
    class OrderPreservation {

        @Test
        @DisplayName("filtered list preserves original "
                + "relative order")
        void filter_whenFiltered_preservesOrder() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();
            List<String> allNames = all.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.COPILOT));
            List<String> filteredNames = result.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();

            // Verify relative order is preserved
            int prevIndex = -1;
            for (String name : filteredNames) {
                int currentIndex =
                        allNames.indexOf(name);
                assertThat(currentIndex)
                        .as("'%s' should come after "
                                + "previous element in "
                                + "original order", name)
                        .isGreaterThan(prevIndex);
                prevIndex = currentIndex;
            }
        }

        @Test
        @DisplayName("ConstitutionAssembler before "
                + "GithubInstructionsAssembler in "
                + "COPILOT filter")
        void filter_copilot_constitutionBeforeGithub() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.COPILOT));
            List<String> names = result.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();

            assertThat(names.indexOf(
                    "ConstitutionAssembler"))
                    .isLessThan(names.indexOf(
                            "GithubInstructionsAssembler"));
        }

        @Test
        @DisplayName("GithubInstructionsAssembler before "
                + "CicdAssembler in COPILOT filter")
        void filter_copilot_githubBeforeCicd() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.COPILOT));
            List<String> names = result.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();

            assertThat(names.indexOf(
                    "GithubInstructionsAssembler"))
                    .isLessThan(names.indexOf(
                            "CicdAssembler"));
        }
    }

    @Nested
    @DisplayName("integration with AssemblerFactory")
    class FactoryIntegration {

        @Test
        @DisplayName("buildAssemblers with CLAUDE_CODE "
                + "options returns filtered list")
        void buildAssemblers_claudeOptions_filtered() {
            PipelineOptions options =
                    new PipelineOptions(
                            false, false, false, false,
                            null,
                            Set.of(Platform.CLAUDE_CODE));

            List<AssemblerDescriptor> result =
                    AssemblerFactory.buildAssemblers(
                            options);

            assertThat(result).hasSize(22);
        }

        @Test
        @DisplayName("buildAssemblers with default "
                + "options returns all 34")
        void buildAssemblers_defaults_returnsAll() {
            List<AssemblerDescriptor> result =
                    AssemblerFactory.buildAssemblers(
                            PipelineOptions.defaults());

            assertThat(result).hasSize(34);
        }

        @Test
        @DisplayName("buildAssemblers no-arg returns "
                + "all 34")
        void buildAssemblers_noArg_returnsAll() {
            List<AssemblerDescriptor> result =
                    AssemblerFactory.buildAssemblers();

            assertThat(result).hasSize(34);
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("empty descriptor list returns "
                + "empty list")
        void filter_emptyList_returnsEmpty() {
            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(
                            List.of(),
                            Set.of(Platform.CLAUDE_CODE));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("SHARED only in platforms still "
                + "filters (SHARED is added to effective)")
        void filter_sharedOnly_returnsSharedOnly() {
            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAssemblers();

            // SHARED is not in allUserSelectable, so
            // shouldSkipFilter is false, but effective
            // set = {SHARED} and only SHARED descriptors
            // match
            List<AssemblerDescriptor> result =
                    PlatformFilter.filter(all,
                            Set.of(Platform.SHARED));

            assertThat(result).hasSize(14);
            assertThat(result).allSatisfy(d ->
                    assertThat(d.platforms()
                            .contains(Platform.SHARED))
                            .isTrue());
        }
    }
}
