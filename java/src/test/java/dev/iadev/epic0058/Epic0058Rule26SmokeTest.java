package dev.iadev.epic0058;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for EPIC-0058, Story 0058-0001: Rule 26 Audit Gate Lifecycle.
 *
 * <p>Validates that Rule 26 exists in the source-of-truth (classpath) with
 * all 8 mandatory sections, that the taxonomy table lists all 4 layers,
 * and that the exit-code table covers codes 0–3.</p>
 */
@DisplayName("Epic0058Rule26SmokeTest — Rule 26 Audit Gate Lifecycle")
class Epic0058Rule26SmokeTest {

    private static final String RULE_26_CLASSPATH =
            "targets/claude/rules/26-audit-gate-lifecycle.md";

    private static final List<String> MANDATORY_SECTIONS = List.of(
            "## Purpose",
            "## Taxonomy",
            "## Naming & Exit Codes",
            "## `--self-check` Flag",
            "## Catalog-before-Add",
            "## Forbidden",
            "## Audit",
            "## Related");

    private static final List<String> TAXONOMY_LAYER_KEYWORDS = List.of(
            "Hook runtime",
            "CI script",
            "Java test",
            "CI workflow");

    private static final List<String> EXIT_CODE_ROWS = List.of(
            "| 0 |",
            "| 1 |",
            "| 2 |",
            "| 3 |");

    private String loadRule26() throws IOException {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(RULE_26_CLASSPATH)) {
            assertThat(is)
                    .as("Rule 26 must be present on classpath at '%s'",
                            RULE_26_CLASSPATH)
                    .isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Rule 26 file exists on classpath (source-of-truth)")
    void rule26_fileExistsOnClasspath() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(RULE_26_CLASSPATH)) {
            assertThat(is)
                    .as("Rule 26 must exist at classpath '%s'", RULE_26_CLASSPATH)
                    .isNotNull();
        } catch (IOException e) {
            throw new AssertionError("Failed to close stream: " + e.getMessage(), e);
        }
    }

    @Test
    @DisplayName("Rule 26 has all 8 mandatory sections")
    void rule26_hasAllMandatorySections() throws IOException {
        String body = loadRule26();
        for (String section : MANDATORY_SECTIONS) {
            assertThat(body)
                    .as("Rule 26 must contain section '%s'", section)
                    .contains(section);
        }
    }

    @Test
    @DisplayName("Rule 26 taxonomy table lists all 4 layers")
    void rule26_taxonomyTableHasFourLayers() throws IOException {
        String body = loadRule26();
        for (String layer : TAXONOMY_LAYER_KEYWORDS) {
            assertThat(body)
                    .as("Rule 26 taxonomy must list layer '%s'", layer)
                    .contains(layer);
        }
    }

    @Test
    @DisplayName("Rule 26 exit-code table covers codes 0, 1, 2, and 3")
    void rule26_exitCodeTableCoversAllCodes() throws IOException {
        String body = loadRule26();
        for (String row : EXIT_CODE_ROWS) {
            assertThat(body)
                    .as("Rule 26 exit-code table must contain row %s", row)
                    .contains(row);
        }
    }

    @Test
    @DisplayName("Rule 26 documents --self-check flag requirement")
    void rule26_documentsSelfCheckFlag() throws IOException {
        String body = loadRule26();
        assertThat(body)
                .as("Rule 26 must document the --self-check flag")
                .contains("--self-check");
    }

    @Test
    @DisplayName("Rule 26 cross-references Rules 19, 21, 22 (ghost script closers)")
    void rule26_referencesGhostScriptRules() throws IOException {
        String body = loadRule26();
        assertThat(body).as("Rule 26 must reference Rule 19").contains("Rule 19");
        assertThat(body).as("Rule 26 must reference Rule 21").contains("Rule 21");
        assertThat(body).as("Rule 26 must reference Rule 22").contains("Rule 22");
    }

    @Test
    @DisplayName("Rule 26 H1 title matches canonical form")
    void rule26_h1TitleMatchesCanonicalForm() throws IOException {
        String body = loadRule26();
        assertThat(body)
                .as("Rule 26 must start with canonical H1 title")
                .contains("# Rule 26 — Audit Gate Lifecycle");
    }
}
