package dev.iadev.generator.templates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the Package Structure section added to
 * {@code _TEMPLATE-IMPLEMENTATION-PLAN.md} by
 * story-0056-0005.
 *
 * <p>Checks: section header present, 5 hexagonal
 * layers listed, dependency direction declared,
 * RA9 alignment note present.</p>
 */
@DisplayName("TemplateImplPlanPackageStructureTest")
class TemplateImplPlanPackageStructureTest {

    private static final Path TEMPLATE = Path.of(
            "src", "main", "resources",
            "shared", "templates",
            "_TEMPLATE-IMPLEMENTATION-PLAN.md");

    @Test
    @DisplayName("implPlan_hasPackageStructureSection")
    void implPlan_hasPackageStructureSection()
            throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("_TEMPLATE-IMPLEMENTATION-PLAN.md must have"
                        + " a ## Package Structure section (story-0056-0005)")
                .contains("## Package Structure");
    }

    @Test
    @DisplayName("implPlan_packageStructure_hasFiveHexagonalLayers")
    void implPlan_packageStructure_hasFiveHexagonalLayers()
            throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Package Structure must list Domain Layer")
                .contains("Domain Layer");
        assertThat(content)
                .as("Package Structure must list Application Layer")
                .contains("Application Layer");
        assertThat(content)
                .as("Package Structure must list Adapter Inbound")
                .contains("Adapter Inbound");
        assertThat(content)
                .as("Package Structure must list Adapter Outbound")
                .contains("Adapter Outbound");
        assertThat(content)
                .as("Package Structure must list Infrastructure")
                .contains("Infrastructure");
    }

    @Test
    @DisplayName("implPlan_packageStructure_declaresDependencyDirection")
    void implPlan_packageStructure_declaresDependencyDirection()
            throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Package Structure must declare dependency direction"
                        + " (adapter → application → domain)")
                .contains("adapter")
                .contains("domain");
    }

    @Test
    @DisplayName("implPlan_packageStructure_hasRa9AlignmentNote")
    void implPlan_packageStructure_hasRa9AlignmentNote()
            throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Package Structure must reference RA9 Section 2"
                        + " for alignment")
                .contains("RA9");
    }

    private String readTemplate() throws IOException {
        return Files.readString(
                TEMPLATE.toAbsolutePath(),
                StandardCharsets.UTF_8);
    }
}
