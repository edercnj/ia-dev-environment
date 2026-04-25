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
 * {@code _TEMPLATE-EPIC.md} v2 (story-0056-0002).
 *
 * <p>Checks: all 9 section headers present and
 * RA9-specific placeholders for Packages + Decision
 * Rationale.</p>
 */
@DisplayName("TemplateEpicV2StructureTest")
class TemplateEpicV2StructureTest {

    private static final Path TEMPLATE = Path.of(
            "src", "main", "resources",
            "shared", "templates", "_TEMPLATE-EPIC.md");

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

    private static final List<String> REQUIRED_PLACEHOLDERS =
            List.of(
                "{{PACKAGES_DOMAIN}}",
                "{{PACKAGES_APPLICATION}}",
                "{{PACKAGES_ADAPTER_INBOUND}}",
                "{{PACKAGES_ADAPTER_OUTBOUND}}",
                "{{PACKAGES_INFRASTRUCTURE}}",
                "{{DECISION_RATIONALE}}"
            );

    @Test
    @DisplayName("epicTemplate_hasAllNineRa9Sections")
    void epicTemplate_hasAllNineRa9Sections()
            throws IOException {
        String content = readTemplate();
        for (String section : REQUIRED_SECTIONS) {
            assertThat(content)
                    .as("_TEMPLATE-EPIC.md v2 must have section: %s",
                            section)
                    .contains(section);
        }
    }

    @Test
    @DisplayName("epicTemplate_hasRa9SpecificPlaceholders")
    void epicTemplate_hasRa9SpecificPlaceholders()
            throws IOException {
        String content = readTemplate();
        for (String placeholder : REQUIRED_PLACEHOLDERS) {
            assertThat(content)
                    .as("_TEMPLATE-EPIC.md must have placeholder: %s",
                            placeholder)
                    .contains(placeholder);
        }
    }

    @Test
    @DisplayName("epicTemplate_hasDecisionRationaleMicroTemplate")
    void epicTemplate_hasDecisionRationaleMicroTemplate()
            throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Epic template must include Decision Rationale"
                        + " micro-template fields")
                .contains("**Decisão:**")
                .contains("**Motivo:**")
                .contains("**Alternativa descartada:**")
                .contains("**Consequência:**");
    }

    @Test
    @DisplayName("epicTemplate_preservesStoryIndexSection")
    void epicTemplate_preservesStoryIndexSection()
            throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Story index must be preserved (in section 9)")
                .contains("story-XXXX-")
                .contains("Dependências");
    }

    private String readTemplate() throws IOException {
        return Files.readString(
                TEMPLATE.toAbsolutePath(),
                StandardCharsets.UTF_8);
    }
}
