package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DagWarningTest {

    @Test
    void create_asymmetricWarning_fieldsAccessible() {
        var warning = new DagWarning(
                DagWarning.Type.ASYMMETRIC_DEPENDENCY,
                "A blocks B, but B missing A in blockedBy");

        assertThat(warning.type())
                .isEqualTo(
                        DagWarning.Type.ASYMMETRIC_DEPENDENCY);
        assertThat(warning.message()).contains("A blocks B");
    }

    @Test
    void create_missingRefWarning_fieldsAccessible() {
        var warning = new DagWarning(
                DagWarning.Type.MISSING_STORY_REFERENCE,
                "Reference to non-existent story");

        assertThat(warning.type())
                .isEqualTo(
                        DagWarning.Type.MISSING_STORY_REFERENCE);
    }

    @Test
    void typeEnum_whenCalled_hasTwoValues() {
        assertThat(DagWarning.Type.values()).hasSize(2);
    }
}
