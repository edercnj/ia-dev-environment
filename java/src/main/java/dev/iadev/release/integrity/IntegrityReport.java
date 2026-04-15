package dev.iadev.release.integrity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregated integrity checks report emitted by {@link IntegrityChecker}.
 *
 * <p>Semantics:
 * <ul>
 *   <li>overallStatus = FAIL when any check is FAIL</li>
 *   <li>overallStatus = WARN when no FAIL but at least one WARN</li>
 *   <li>overallStatus = PASS when all checks PASS</li>
 *   <li>errorCode is present ONLY when overallStatus == FAIL</li>
 * </ul>
 * WARN alone never aborts the release (Story 0039-0003 §7, WARN boundary).</p>
 */
public record IntegrityReport(List<CheckResult> checks, CheckStatus overallStatus, Optional<String> errorCode) {

    public static final String ERROR_CODE = "VALIDATE_INTEGRITY_DRIFT";

    public IntegrityReport {
        Objects.requireNonNull(checks, "checks");
        Objects.requireNonNull(overallStatus, "overallStatus");
        Objects.requireNonNull(errorCode, "errorCode");
        checks = List.copyOf(checks);
    }

    /**
     * Compose an {@link IntegrityReport} from the individual {@link CheckResult}s.
     *
     * @param results ordered list of individual check results; must not be null
     * @return aggregated report with derived overallStatus and errorCode
     */
    public static IntegrityReport aggregate(List<CheckResult> results) {
        Objects.requireNonNull(results, "results");
        boolean hasFail = results.stream().anyMatch(r -> r.status() == CheckStatus.FAIL);
        boolean hasWarn = results.stream().anyMatch(r -> r.status() == CheckStatus.WARN);
        CheckStatus overall;
        Optional<String> code;
        if (hasFail) {
            overall = CheckStatus.FAIL;
            code = Optional.of(ERROR_CODE);
        } else if (hasWarn) {
            overall = CheckStatus.WARN;
            code = Optional.empty();
        } else {
            overall = CheckStatus.PASS;
            code = Optional.empty();
        }
        return new IntegrityReport(results, overall, code);
    }
}
