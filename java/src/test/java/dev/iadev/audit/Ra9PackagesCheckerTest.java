package dev.iadev.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Ra9PackagesChecker}
 * (TASK-0056-0007-003).
 *
 * <p>Checks that section 2 (Packages Hexagonal) has at
 * least one non-dash layer entry.</p>
 */
@DisplayName("Ra9PackagesCheckerTest")
class Ra9PackagesCheckerTest {

    private static final String SECTION_HEADER =
            "## 2. Packages (Hexagonal)\n\n";

    private final Ra9PackagesChecker checker =
            new Ra9PackagesChecker();

    @Test
    @DisplayName("check_atLeastOneLayer_noViolations")
    void check_atLeastOneLayer_noViolations() {
        String content = SECTION_HEADER
                + "### Domain Layer\n"
                + "- `domain/payment/model/PaymentOrder.java`\n";
        List<String> violations = checker.check(
                content, "epic-0060.md");
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("check_allLayersDash_flagsViolation")
    void check_allLayersDash_flagsViolation() {
        String content = SECTION_HEADER
                + "### Domain Layer\n\n— \n\n"
                + "### Application Layer\n\n— \n\n"
                + "### Adapter Inbound\n\n— \n\n"
                + "### Adapter Outbound\n\n— \n\n"
                + "### Infrastructure\n\n— \n\n";
        List<String> violations = checker.check(
                content, "story-0060-0001.md");
        assertThat(violations)
                .as("All-dash layers must flag RA9_PACKAGES_MISSING")
                .isNotEmpty();
        assertThat(violations.get(0))
                .contains("RA9_PACKAGES_MISSING");
    }

    @Test
    @DisplayName("check_missingSection2_noViolation")
    void check_missingSection2_noViolation() {
        String content = "## 1. Contexto & Escopo\ncontent\n";
        List<String> violations = checker.check(
                content, "story.md");
        assertThat(violations)
                .as("Missing section 2 is caught by Ra9SectionsChecker")
                .isEmpty();
    }

    @Test
    @DisplayName("check_auditExempt_noViolations")
    void check_auditExempt_noViolations() {
        String content = "<!-- audit-exempt -->\n"
                + SECTION_HEADER + "— \n— \n— \n— \n— \n";
        List<String> violations = checker.check(
                content, "legacy.md");
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("check_oneLayerFilled_noViolations")
    void check_oneLayerFilled_oneLayerEnough() {
        String content = SECTION_HEADER
                + "### Domain Layer\n"
                + "- `domain/payment/Payment.java` — entity\n\n"
                + "### Application Layer\n\n— \n\n"
                + "### Adapter Inbound\n\n— \n\n"
                + "### Adapter Outbound\n\n— \n\n"
                + "### Infrastructure\n\n— \n\n";
        List<String> violations = checker.check(
                content, "story-0060.md");
        assertThat(violations)
                .as("At least 1 non-dash layer means no violation")
                .isEmpty();
    }
}
