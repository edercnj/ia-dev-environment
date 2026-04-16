package dev.iadev.release.dryrun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultPhaseCatalog}.
 */
@DisplayName("DefaultPhaseCatalogTest")
class DefaultPhaseCatalogTest {

    @Test
    @DisplayName("phases_returnsThirteenOrderedPhases")
    void phases_returnsThirteenOrderedPhases() {
        DefaultPhaseCatalog catalog = new DefaultPhaseCatalog();

        List<PhaseDescriptor> phases = catalog.phases();

        assertThat(phases).hasSize(13);
        assertThat(phases.get(0).name()).isEqualTo("INITIALIZED");
        assertThat(phases.get(12).name()).isEqualTo("CLEANED");
    }

    @Test
    @DisplayName("phases_everyPhaseHasAtLeastOneCommand")
    void phases_everyPhaseHasAtLeastOneCommand() {
        DefaultPhaseCatalog catalog = new DefaultPhaseCatalog();

        List<PhaseDescriptor> phases = catalog.phases();

        assertThat(phases)
                .allSatisfy(p -> assertThat(p.commands())
                        .as("commands for phase %s", p.name())
                        .isNotEmpty());
    }

    @Test
    @DisplayName("phases_returnsImmutableList")
    void phases_returnsImmutableList() {
        DefaultPhaseCatalog catalog = new DefaultPhaseCatalog();

        List<PhaseDescriptor> phases = catalog.phases();

        assertThat(phases).isUnmodifiable();
    }
}
