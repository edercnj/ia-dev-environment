package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the AssemblerDescriptor record.
 */
@DisplayName("AssemblerDescriptor")
class AssemblerDescriptorTest {

    @Test
    @DisplayName("stores name, target, platforms, and "
            + "assembler instance")
    void constructor_whenCalled_storesAllFields() {
        Assembler mockAssembler = (c, e, p) -> List.of();
        Set<Platform> platforms =
                Set.of(Platform.CLAUDE_CODE);

        var descriptor = new AssemblerDescriptor(
                "RulesAssembler",
                AssemblerTarget.CLAUDE,
                platforms,
                mockAssembler);

        assertThat(descriptor.name())
                .isEqualTo("RulesAssembler");
        assertThat(descriptor.target())
                .isEqualTo(AssemblerTarget.CLAUDE);
        assertThat(descriptor.platforms())
                .containsExactly(Platform.CLAUDE_CODE);
        assertThat(descriptor.assembler())
                .isSameAs(mockAssembler);
    }

    @Test
    @DisplayName("platforms set is immutable")
    void constructor_whenCalled_platformsAreImmutable() {
        var descriptor = new AssemblerDescriptor(
                "Test",
                AssemblerTarget.ROOT,
                Set.of(Platform.SHARED),
                (c, e, p) -> List.of());

        assertThat(descriptor.platforms())
                .isUnmodifiable();
    }

    @Test
    @DisplayName("equals and hashCode based on all fields")
    void equalsHashCode_whenCalled_basedOnAllFields() {
        Assembler assembler = (c, e, p) -> List.of();
        Set<Platform> platforms =
                Set.of(Platform.CLAUDE_CODE);

        var d1 = new AssemblerDescriptor(
                "Skills", AssemblerTarget.CLAUDE,
                platforms, assembler);
        var d2 = new AssemblerDescriptor(
                "Skills", AssemblerTarget.CLAUDE,
                platforms, assembler);

        assertThat(d1).isEqualTo(d2);
        assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
    }

    @Test
    @DisplayName("toString contains name")
    void toString_whenCalled_containsName() {
        var descriptor = new AssemblerDescriptor(
                "AgentsAssembler",
                AssemblerTarget.CLAUDE,
                Set.of(Platform.CLAUDE_CODE),
                (c, e, p) -> List.of());

        assertThat(descriptor.toString())
                .contains("AgentsAssembler");
    }
}
