package dev.iadev.epic0058;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for EPIC-0058, Story 0058-0002: Audit Gates Catalog consistency.
 *
 * <p>Validates that:
 * <ul>
 *   <li>docs/audit-gates-catalog.md exists with ≥ 8 gates in the master table</li>
 *   <li>All 7 target Rules contain a catalog cross-ref line</li>
 *   <li>No Rule references audit-*.sh without a catalog cross-ref</li>
 * </ul>
 * </p>
 */
@DisplayName("Epic0058CatalogConsistencySmokeTest — Audit Gates Catalog")
class Epic0058CatalogConsistencySmokeTest {

    private static final Path REPO_ROOT = Paths.get("..").toAbsolutePath().normalize();
    private static final Path CATALOG_PATH = REPO_ROOT.resolve("docs/audit-gates-catalog.md");
    private static final Path RULES_SOURCE_DIR = Paths.get(
            "src/main/resources/targets/claude/rules");

    private static final String CATALOG_CROSSREF_MARKER =
            "Catalogado em:";

    private static final List<String> RULES_REQUIRING_CROSSREF = List.of(
            "13-skill-invocation-protocol.md",
            "19-backward-compatibility.md",
            "21-epic-branch-model.md",
            "22-skill-visibility.md",
            "23-model-selection.md",
            "24-execution-integrity.md",
            "25-task-hierarchy.md");

    // -----------------------------------------------------------------------
    // Scenario: Catálogo inexistente (degenerate)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("docs/audit-gates-catalog.md exists in repository")
    void catalog_fileExists() {
        assertThat(CATALOG_PATH)
                .as("docs/audit-gates-catalog.md must exist at %s", CATALOG_PATH)
                .exists()
                .isRegularFile();
    }

    // -----------------------------------------------------------------------
    // Scenario: Catálogo com todos os 8 gates (happy path)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Master table in catalog has >= 8 gate entries")
    void catalog_masterTableHasAtLeast8Gates() throws IOException {
        assertThat(CATALOG_PATH).exists();
        String body = Files.readString(CATALOG_PATH, StandardCharsets.UTF_8);

        long tableRows = body.lines()
                .filter(line -> line.startsWith("| `audit-") || line.startsWith("| `verify-"))
                .count();

        assertThat(tableRows)
                .as("Master table must have >= 8 gate entries (found %d)", tableRows)
                .isGreaterThanOrEqualTo(8L);
    }

    @Test
    @DisplayName("All 7 target Rules have catalog cross-ref line")
    void rules_allTargetRulesHaveCatalogCrossRef() throws IOException {
        List<String> missingCrossRef = RULES_REQUIRING_CROSSREF.stream()
                .filter(ruleName -> {
                    Path rulePath = RULES_SOURCE_DIR.resolve(ruleName);
                    try {
                        String body = Files.readString(rulePath, StandardCharsets.UTF_8);
                        return !body.contains(CATALOG_CROSSREF_MARKER);
                    } catch (IOException e) {
                        return true;
                    }
                })
                .collect(Collectors.toList());

        assertThat(missingCrossRef)
                .as("Rules missing catalog cross-ref: %s", missingCrossRef)
                .isEmpty();
    }

    // -----------------------------------------------------------------------
    // Scenario: Rule menciona script sem cross-ref (error)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("No Rule references audit-*.sh without catalog cross-ref")
    void rules_noOrphanAuditScriptReferences() throws IOException {
        List<String> violatingRules = RULES_REQUIRING_CROSSREF.stream()
                .filter(ruleName -> {
                    Path rulePath = RULES_SOURCE_DIR.resolve(ruleName);
                    try {
                        String body = Files.readString(rulePath, StandardCharsets.UTF_8);
                        boolean mentionsAuditScript = body.contains("scripts/audit-")
                                || body.contains("audit-flow-version")
                                || body.contains("audit-epic-branches")
                                || body.contains("audit-skill-visibility")
                                || body.contains("audit-model-selection")
                                || body.contains("audit-execution-integrity")
                                || body.contains("audit-task-hierarchy")
                                || body.contains("audit-phase-gates");
                        boolean hasCrossRef = body.contains(CATALOG_CROSSREF_MARKER);
                        return mentionsAuditScript && !hasCrossRef;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .map(name -> "RULE_MISSING_CATALOG_BACKREF: " + name)
                .collect(Collectors.toList());

        assertThat(violatingRules)
                .as("Rules that reference audit scripts but lack catalog cross-ref")
                .isEmpty();
    }

    // -----------------------------------------------------------------------
    // Scenario: Catálogo lista gate (boundary — validate catalog format)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Catalog master table rows use required column format")
    void catalog_masterTableRowsHaveRequiredColumns() throws IOException {
        assertThat(CATALOG_PATH).exists();
        String body = Files.readString(CATALOG_PATH, StandardCharsets.UTF_8);

        List<String> tableRows = body.lines()
                .filter(line -> line.startsWith("| `audit-") || line.startsWith("| `verify-"))
                .collect(Collectors.toList());

        assertThat(tableRows).isNotEmpty();

        for (String row : tableRows) {
            long pipeCount = row.chars().filter(c -> c == '|').count();
            assertThat(pipeCount)
                    .as("Table row must have 7+ column separators: %s", row)
                    .isGreaterThanOrEqualTo(7L);
        }
    }

    @Test
    @DisplayName("Catalog contains 'How to Read' section and layer taxonomy")
    void catalog_hasRequiredSections() throws IOException {
        assertThat(CATALOG_PATH).exists();
        String body = Files.readString(CATALOG_PATH, StandardCharsets.UTF_8);

        assertThat(body).as("Catalog must explain how to read it").contains("How to Read");
        assertThat(body).as("Catalog must reference Hook runtime layer").contains("Hook runtime");
        assertThat(body).as("Catalog must reference CI script layer").contains("CI script");
        assertThat(body).as("Catalog must reference Java test layer").contains("Java test");
    }
}
