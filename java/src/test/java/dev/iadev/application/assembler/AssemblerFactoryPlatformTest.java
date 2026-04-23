package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests validating platform assignment for all assemblers.
 */
@DisplayName("AssemblerFactory — Platform mapping")
class AssemblerFactoryPlatformTest {

    private static final List<String> CLAUDE_CODE_NAMES =
            List.of(
                    "RulesAssembler",
                    "KnowledgeAssembler",
                    "SkillsAssembler",
                    "AgentsAssembler",
                    "PatternsAssembler",
                    "ProtocolsAssembler",
                    "HooksAssembler",
                    "SettingsAssembler",
                    "ReadmeAssembler",
                    "ClaudeMdAssembler");

    private static final List<String> SHARED_NAMES =
            List.of(
                    "ConstitutionAssembler",
                    "DocsAssembler",
                    "GrpcDocsAssembler",
                    "RunbookAssembler",
                    "IncidentTemplatesAssembler",
                    "ReleaseChecklistAssembler",
                    "OperationalRunbookAssembler",
                    "SloSliTemplateAssembler",
                    "DocsContributingAssembler",
                    "DataMigrationPlanAssembler",
                    "CicdAssembler",
                    "EpicReportAssembler",
                    "DocsAdrAssembler",
                    "PlanTemplatesAssembler");

    @Nested
    @DisplayName("Platform counts")
    class PlatformCounts {

        @Test
        @DisplayName("total assembler count is 24")
        void buildAssemblers_totalCount_is23() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerFactory.buildAssemblers();

            assertThat(descriptors).hasSize(24);
        }

        @Test
        @DisplayName("10 assemblers have CLAUDE_CODE "
                + "platform")
        void buildAssemblers_claudeCodeCount_is9() {
            List<String> claudeNames =
                    filterByPlatform(Platform.CLAUDE_CODE);

            assertThat(claudeNames)
                    .as("CLAUDE_CODE assemblers")
                    .hasSize(10)
                    .containsExactlyInAnyOrderElementsOf(
                            CLAUDE_CODE_NAMES);
        }

        @Test
        @DisplayName("14 assemblers have SHARED platform")
        void buildAssemblers_sharedCount_is14() {
            List<String> sharedNames =
                    filterByPlatform(Platform.SHARED);

            assertThat(sharedNames)
                    .as("SHARED assemblers")
                    .hasSize(14)
                    .containsExactlyInAnyOrderElementsOf(
                            SHARED_NAMES);
        }

        @Test
        @DisplayName("sum of platform counts equals total")
        void buildAssemblers_platformSum_equalsTotal() {
            int claude =
                    filterByPlatform(Platform.CLAUDE_CODE)
                            .size();
            int shared =
                    filterByPlatform(Platform.SHARED).size();

            assertThat(claude + shared)
                    .as("10 + 14 = 24")
                    .isEqualTo(24);
        }
    }

    @Nested
    @DisplayName("Platform invariants")
    class PlatformInvariants {

        @Test
        @DisplayName("every assembler has at least one "
                + "platform")
        void buildAssemblers_allHaveNonEmptyPlatforms() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerFactory.buildAssemblers();

            for (AssemblerDescriptor desc : descriptors) {
                assertThat(desc.platforms())
                        .as("platforms for %s", desc.name())
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("each assembler belongs to exactly "
                + "one platform")
        void buildAssemblers_eachHasExactlyOnePlatform() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerFactory.buildAssemblers();

            for (AssemblerDescriptor desc : descriptors) {
                assertThat(desc.platforms())
                        .as("platforms for %s should have "
                                + "exactly 1 element",
                                desc.name())
                        .hasSize(1);
            }
        }

        @Test
        @DisplayName("SHARED assemblers have only SHARED "
                + "platform")
        void buildAssemblers_sharedOnlyHasShared() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerFactory.buildAssemblers();

            for (AssemblerDescriptor desc : descriptors) {
                if (desc.platforms()
                        .contains(Platform.SHARED)) {
                    assertThat(desc.platforms())
                            .as("SHARED assembler %s must "
                                    + "not combine with "
                                    + "other platforms",
                                    desc.name())
                            .containsOnly(Platform.SHARED);
                }
            }
        }

        @Test
        @DisplayName("platforms set is immutable")
        void buildAssemblers_platformsAreImmutable() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerFactory.buildAssemblers();

            for (AssemblerDescriptor desc : descriptors) {
                assertThat(desc.platforms())
                        .as("platforms for %s", desc.name())
                        .isUnmodifiable();
            }
        }
    }

    @Nested
    @DisplayName("Specific assembler mappings")
    class SpecificMappings {

        @Test
        @DisplayName("ReadmeAssembler has CLAUDE_CODE "
                + "platform")
        void readmeAssembler_hasClaude() {
            AssemblerDescriptor readme = findByName(
                    "ReadmeAssembler");

            assertThat(readme.platforms())
                    .containsExactly(Platform.CLAUDE_CODE);
        }

        @Test
        @DisplayName("ConstitutionAssembler has SHARED "
                + "platform")
        void constitutionAssembler_hasShared() {
            AssemblerDescriptor constitution = findByName(
                    "ConstitutionAssembler");

            assertThat(constitution.platforms())
                    .containsExactly(Platform.SHARED);
        }

        @Test
        @DisplayName("DocsAdrAssembler has SHARED "
                + "platform")
        void docsAdrAssembler_hasShared() {
            AssemblerDescriptor adr = findByName(
                    "DocsAdrAssembler");

            assertThat(adr.platforms())
                    .containsExactly(Platform.SHARED);
        }

        @Test
        @DisplayName("PlanTemplatesAssembler has SHARED "
                + "platform")
        void planTemplatesAssembler_hasShared() {
            AssemblerDescriptor templates = findByName(
                    "PlanTemplatesAssembler");

            assertThat(templates.platforms())
                    .containsExactly(Platform.SHARED);
        }
    }

    @Nested
    @DisplayName("No assembler overlap")
    class NoOverlap {

        @Test
        @DisplayName("no assembler appears in multiple "
                + "platform groups")
        void buildAssemblers_noOverlapBetweenPlatforms() {
            Map<Platform, List<String>> byPlatform =
                    AssemblerFactory.buildAssemblers()
                            .stream()
                            .flatMap(d -> d.platforms()
                                    .stream()
                                    .map(platform ->
                                            Map.entry(
                                                    platform,
                                                    d.name())))
                            .collect(Collectors.groupingBy(
                                    Map.Entry::getKey,
                                    Collectors.mapping(
                                            Map.Entry::getValue,
                                            Collectors
                                                    .toList()
                                    )));

            Set<String> allNames = byPlatform.values()
                    .stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toSet());

            long totalCount = byPlatform.values().stream()
                    .mapToLong(List::size)
                    .sum();

            assertThat(totalCount)
                    .as("No duplicates across platforms")
                    .isEqualTo(allNames.size());
        }
    }

    // --- helpers ---

    private static List<String> filterByPlatform(
            Platform platform) {
        return AssemblerFactory.buildAssemblers().stream()
                .filter(d -> d.platforms()
                        .contains(platform))
                .map(AssemblerDescriptor::name)
                .toList();
    }

    private static AssemblerDescriptor findByName(
            String name) {
        return AssemblerFactory.buildAssemblers().stream()
                .filter(d -> d.name().equals(name))
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Assembler not found: "
                                        + name));
    }
}
