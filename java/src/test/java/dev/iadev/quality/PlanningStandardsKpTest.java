package dev.iadev.quality;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates structural invariants of the
 * {@code planning-standards-kp} SKILL.md (story-0056-0001).
 *
 * <p>Checks: frontmatter contract ({@code user-invocable: false},
 * {@code name: planning-standards-kp}) and presence of all
 * 9 RA9 section headers.</p>
 */
@DisplayName("PlanningStandardsKpTest")
class PlanningStandardsKpTest {

    private static final Path KP_FILE = Path.of(
            "src", "main", "resources",
            "targets", "claude", "skills", "core",
            "plan", "planning-standards-kp", "SKILL.md");

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
    @DisplayName("kp_exists_atExpectedPath")
    void kp_exists_atExpectedPath() {
        assertThat(KP_FILE.toAbsolutePath())
                .as("planning-standards-kp SKILL.md must exist"
                        + " — create it via story-0056-0001")
                .exists()
                .isRegularFile();
    }

    @Test
    @DisplayName("kp_frontmatter_hasUserInvocableFalse")
    void kp_frontmatter_hasUserInvocableFalse()
            throws IOException {
        String content = readKp();
        assertThat(content)
                .as("Frontmatter must have name: planning-standards-kp")
                .contains("name: planning-standards-kp");
        assertThat(content)
                .as("Frontmatter must have user-invocable: false"
                        + " (KP is not user-invocable)")
                .contains("user-invocable: false");
    }

    @Test
    @DisplayName("kp_hasAllNineRa9Sections")
    void kp_hasAllNineRa9Sections() throws IOException {
        String content = readKp();
        for (String section : REQUIRED_SECTIONS) {
            assertThat(content)
                    .as("KP must contain section: %s", section)
                    .contains(section);
        }
    }

    @Test
    @DisplayName("kp_hasDecisionRationaleMicroTemplate")
    void kp_hasDecisionRationaleMicroTemplate()
            throws IOException {
        String content = readKp();
        assertThat(content)
                .as("KP must include Decision Rationale"
                        + " micro-template with **Decisão:** field")
                .contains("**Decisão:**");
        assertThat(content)
                .as("KP must include **Motivo:** field")
                .contains("**Motivo:**");
        assertThat(content)
                .as("KP must include **Alternativa descartada:** field")
                .contains("**Alternativa descartada:**");
        assertThat(content)
                .as("KP must include **Consequência:** field")
                .contains("**Consequência:**");
    }

    @Test
    @DisplayName("kp_hasGranularityTable_forAllThreeLevels")
    void kp_hasGranularityTable_forAllThreeLevels()
            throws IOException {
        String content = readKp();
        assertThat(content)
                .as("KP must document Epic granularity")
                .contains("Epic");
        assertThat(content)
                .as("KP must document Story granularity")
                .contains("Story");
        assertThat(content)
                .as("KP must document Task granularity")
                .contains("Task");
    }

    private String readKp() throws IOException {
        return Files.readString(
                KP_FILE.toAbsolutePath(),
                StandardCharsets.UTF_8);
    }
}
