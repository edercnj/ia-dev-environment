package dev.iadev.architecture;

import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the baseline audit and verifies it produces results.
 * This test never fails the build -- it only documents
 * violations for the baseline report.
 */
@Tag("architecture-audit")
class HexagonalArchitectureBaselineAuditTest {

    @Test
    void auditProducesResults() {
        Map<String, EvaluationResult> results =
            HexagonalArchitectureBaselineAudit.runAudit();

        assertThat(results).hasSize(7);
    }
}
