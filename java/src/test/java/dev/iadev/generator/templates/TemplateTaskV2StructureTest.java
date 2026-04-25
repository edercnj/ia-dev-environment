package dev.iadev.generator.templates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates RA9 structural invariants of
 * {@code _TEMPLATE-TASK.md} v2 (story-0056-0004).
 *
 * <p>Checks: all 9 section headers present, Decision
 * Rationale accepts N/A, and Packages section lists
 * 1-3 specific files.</p>
 */
@DisplayName("TemplateTaskV2StructureTest")
class TemplateTaskV2StructureTest {

    private static final Path TEMPLATE = Path.of(
            "src", "main", "resources",
            "shared", "templates", "_TEMPLATE-TASK.md");

    private static final List<String> REQUIRED_SECTIONS =
            List.of(
                "## 1. Contexto & Escopo",
                "## 2. Packages (Hexagonal)",
                "## 3. Contratos & Endpoints",
                "## 4. Materialização SOLID",
                "## 5. Quality Gates",
                "## 6. Segurança",
                "## 7. Observabilidade",
                "## 8. Decision Rationale",
                "## 9. Dependências & File Footprint"
            );

    @Test
    @DisplayName("taskTemplate_hasAllNineRa9Sections")
    void taskTemplate_hasAllNineRa9Sections()
            throws IOException {
        String content = readTemplate();
        for (String section : REQUIRED_SECTIONS) {
            assertThat(content)
                    .as("_TEMPLATE-TASK.md v2 must have section: %s",
                            section)
                    .contains(section);
        }
    }

    @Test
    @DisplayName("taskTemplate_hasPackagesSectionForFiles")
    void taskTemplate_hasPackagesSectionForFiles()
            throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Task Packages section must list 1-3 specific files")
                .contains("## 2. Packages (Hexagonal)")
                .contains("Layer")
                .contains(".java");
    }

    @Test
    @DisplayName("taskTemplate_decisionRationaleAcceptsNa")
    void taskTemplate_decisionRationaleAcceptsNa()
            throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Task Decision Rationale must accept N/A")
                .contains("N/A");
        assertThat(content)
                .as("Task Decision Rationale section must exist")
                .contains("## 8. Decision Rationale");
    }

    @Test
    @DisplayName("taskTemplate_hasIoContractInSection3")
    void taskTemplate_hasIoContractInSection3()
            throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("I/O contract must be in section 3"
                        + " (Contratos & Endpoints)")
                .contains("## 3. Contratos & Endpoints")
                .contains("Inputs")
                .contains("Outputs");
    }

    @Test
    @DisplayName("taskTemplate_hasDodInSection5")
    void taskTemplate_hasDodInSection5()
            throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("DoD checklist must be in section 5"
                        + " (Quality Gates)")
                .contains("## 5. Quality Gates")
                .contains("{{COMPILE_COMMAND}}")
                .contains("Red → Green → Refactor");
    }

    @Test
    @DisplayName("taskTemplate_hasDependenciesInSection9")
    void taskTemplate_hasDependenciesInSection9()
            throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Dependencies must be in section 9")
                .contains("## 9. Dependências & File Footprint")
                .contains("Depends on");
    }

    private String readTemplate() throws IOException {
        return Files.readString(
                TEMPLATE.toAbsolutePath(),
                StandardCharsets.UTF_8);
    }
}
