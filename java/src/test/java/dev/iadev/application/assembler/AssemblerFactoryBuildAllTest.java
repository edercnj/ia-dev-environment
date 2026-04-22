package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AssemblerFactory#buildAllAssemblers}.
 */
@DisplayName("AssemblerFactory — buildAllAssemblers")
class AssemblerFactoryBuildAllTest {

    @Nested
    @DisplayName("unfiltered list")
    class UnfilteredList {

        @Test
        @DisplayName("returns all 23 assemblers regardless "
                + "of platform option")
        void buildAllAssemblers_withClaudeFilter_returns23() {
            PipelineOptions options =
                    new PipelineOptions(
                            false, false, false,
                            false, null,
                            Set.of(Platform.CLAUDE_CODE));

            List<AssemblerDescriptor> result =
                    AssemblerFactory.buildAllAssemblers(
                            options);

            assertThat(result).hasSize(23);
        }

        @Test
        @DisplayName("returns all 23 with empty platforms")
        void buildAllAssemblers_emptyPlatforms_returns23() {
            PipelineOptions options =
                    PipelineOptions.defaults();

            List<AssemblerDescriptor> result =
                    AssemblerFactory.buildAllAssemblers(
                            options);

            assertThat(result).hasSize(23);
        }

        @Test
        @DisplayName("returns immutable list")
        void buildAllAssemblers_returnsImmutableList() {
            List<AssemblerDescriptor> result =
                    AssemblerFactory.buildAllAssemblers(
                            PipelineOptions.defaults());

            assertThat(result).isUnmodifiable();
        }

        @Test
        @DisplayName("contains all remaining platform "
                + "categories")
        void buildAllAssemblers_containsAllPlatforms() {
            List<AssemblerDescriptor> result =
                    AssemblerFactory.buildAllAssemblers(
                            PipelineOptions.defaults());

            boolean hasClaude = result.stream()
                    .anyMatch(d -> d.platforms()
                            .contains(Platform.CLAUDE_CODE));
            boolean hasShared = result.stream()
                    .anyMatch(d -> d.platforms()
                            .contains(Platform.SHARED));

            assertThat(hasClaude).isTrue();
            assertThat(hasShared).isTrue();
        }
    }

    @Nested
    @DisplayName("consistency with buildAssemblers")
    class Consistency {

        @Test
        @DisplayName("buildAllAssemblers is superset of "
                + "buildAssemblers with filter")
        void buildAllAssemblers_isSupersetOfFiltered() {
            PipelineOptions options =
                    new PipelineOptions(
                            false, false, false,
                            false, null,
                            Set.of(Platform.CLAUDE_CODE));

            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAllAssemblers(
                            options);
            List<AssemblerDescriptor> filtered =
                    AssemblerFactory.buildAssemblers(
                            options);

            List<String> allNames = all.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();
            List<String> filteredNames = filtered.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();

            assertThat(allNames)
                    .containsAll(filteredNames);
        }

        @Test
        @DisplayName("with no filter, buildAssemblers "
                + "equals buildAllAssemblers")
        void noFilter_buildAssemblers_equalsBuildAll() {
            PipelineOptions options =
                    PipelineOptions.defaults();

            List<AssemblerDescriptor> all =
                    AssemblerFactory.buildAllAssemblers(
                            options);
            List<AssemblerDescriptor> filtered =
                    AssemblerFactory.buildAssemblers(
                            options);

            assertThat(filtered).hasSize(all.size());
        }
    }
}
