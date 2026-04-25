package dev.iadev.smoke;

import dev.iadev.audit.Ra9PackagesChecker;
import dev.iadev.audit.Ra9RationaleChecker;
import dev.iadev.audit.Ra9SectionsChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke / E2E tests for the RA9 audit pipeline
 * (TASK-0056-0007-005).
 *
 * <p>Validates that an intentionally invalid plan fixture
 * produces violations, and that a compliant plan passes.</p>
 */
@DisplayName("Ra9AuditSmokeTest")
class Ra9AuditSmokeTest {

    private static final String INVALID_FIXTURE =
            "/fixtures/ra9-invalid-plan.md";

    private final Ra9SectionsChecker sectionsChecker =
            new Ra9SectionsChecker();
    private final Ra9RationaleChecker rationaleChecker =
            new Ra9RationaleChecker();
    private final Ra9PackagesChecker packagesChecker =
            new Ra9PackagesChecker();

    @Test
    @DisplayName("invalidPlan_fixture_producesViolations")
    void invalidPlan_fixture_producesViolations()
            throws IOException {
        String content = loadFixture(INVALID_FIXTURE);
        List<String> violations = sectionsChecker.check(
                content, "ra9-invalid-plan.md");
        assertThat(violations)
                .as("Invalid fixture must produce RA9_SECTIONS_MISSING"
                        + " violations for each missing section")
                .isNotEmpty();
        assertThat(violations)
                .allMatch(v -> v.contains("RA9_SECTIONS_MISSING"),
                        "all violations must carry RA9_SECTIONS_MISSING code");
    }

    @Test
    @DisplayName("invalidPlan_fixture_missingAtLeastEightSections")
    void invalidPlan_fixture_missingAtLeastEightSections()
            throws IOException {
        String content = loadFixture(INVALID_FIXTURE);
        List<String> violations = sectionsChecker.check(
                content, "ra9-invalid-plan.md");
        assertThat(violations.size())
                .as("Invalid plan has only legacy section 1 renamed,"
                        + " must flag at least 8 missing sections")
                .isGreaterThanOrEqualTo(8);
    }

    @Test
    @DisplayName("compliantPlan_inline_producesNoViolations")
    void compliantPlan_inline_producesNoViolations() {
        String compliant = "# História\n"
                + "## 1. Contexto & Escopo\ncontent\n"
                + "## 2. Packages (Hexagonal)\n"
                + "### Domain Layer\n- `domain/X.java`\n"
                + "## 3. Contratos & Endpoints\ncontent\n"
                + "## 4. Materialização SOLID\ncontent\n"
                + "## 5. Quality Gates\ncontent\n"
                + "## 6. Segurança\ncontent\n"
                + "## 7. Observabilidade\ncontent\n"
                + "## 8. Decision Rationale\n"
                + "**Decisão:** Use JPA.\n**Motivo:** Less boilerplate.\n"
                + "**Alternativa descartada:** JDBC.\n**Consequência:** Spring dependency.\n"
                + "## 9. Dependências & File Footprint\ncontent\n";

        assertThat(sectionsChecker.check(compliant, "test.md"))
                .isEmpty();
        assertThat(rationaleChecker.check(compliant, "test.md"))
                .isEmpty();
        assertThat(packagesChecker.check(compliant, "test.md"))
                .isEmpty();
    }

    private String loadFixture(String resource)
            throws IOException {
        try (InputStream is = Ra9AuditSmokeTest.class
                .getResourceAsStream(resource)) {
            assertThat(is)
                    .as("Fixture %s must exist on classpath",
                            resource)
                    .isNotNull();
            return new String(
                    is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
