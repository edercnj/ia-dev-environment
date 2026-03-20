package dev.iadev.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the AssemblerDescriptor record.
 */
@DisplayName("AssemblerDescriptor")
class AssemblerDescriptorTest {

    @Test
    @DisplayName("stores name, target, and assembler instance")
    void constructor_whenCalled_storesAllFields() {
        Assembler mockAssembler = (c, e, p) -> List.of();

        var descriptor = new AssemblerDescriptor(
                "RulesAssembler",
                AssemblerTarget.CLAUDE,
                mockAssembler);

        assertThat(descriptor.name())
                .isEqualTo("RulesAssembler");
        assertThat(descriptor.target())
                .isEqualTo(AssemblerTarget.CLAUDE);
        assertThat(descriptor.assembler())
                .isSameAs(mockAssembler);
    }

    @Test
    @DisplayName("equals and hashCode based on all fields")
    void equalsHashCode_whenCalled_basedOnAllFields() {
        Assembler assembler = (c, e, p) -> List.of();

        var d1 = new AssemblerDescriptor(
                "Skills", AssemblerTarget.CLAUDE, assembler);
        var d2 = new AssemblerDescriptor(
                "Skills", AssemblerTarget.CLAUDE, assembler);

        assertThat(d1).isEqualTo(d2);
        assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
    }

    @Test
    @DisplayName("toString contains name")
    void toString_whenCalled_containsName() {
        var descriptor = new AssemblerDescriptor(
                "AgentsAssembler",
                AssemblerTarget.CLAUDE,
                (c, e, p) -> List.of());

        assertThat(descriptor.toString())
                .contains("AgentsAssembler");
    }
}
