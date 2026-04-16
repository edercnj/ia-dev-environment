package dev.iadev.release.preflight;

import dev.iadev.release.integrity.CheckStatus;
import dev.iadev.release.integrity.IntegrityReport;

import java.util.Objects;

/**
 * Orchestrates the pre-flight phase (story-0039-0009 §3.3).
 *
 * <p>Positioned between Step 1 (DETERMINE) and Step 2 (VALIDATE-DEEP).
 * Reuses output from S01 (version detection) and S03 (integrity checks)
 * to compose a dashboard and determine the release disposition.</p>
 *
 * <p>Flow:
 * <ol>
 *   <li>Render dashboard from {@link DashboardData}</li>
 *   <li>If integrity FAIL: return {@link PreflightResult#integrityFail} (no prompt)</li>
 *   <li>Otherwise: return the dashboard and let the caller prompt the operator</li>
 * </ol></p>
 */
public final class PreflightOrchestrator {

    private PreflightOrchestrator() {
        throw new AssertionError("no instances");
    }

    /**
     * Evaluates the pre-flight data and produces the dashboard.
     *
     * <p>When integrity checks FAIL, the result signals an immediate abort
     * without offering the operator a prompt (story-0039-0009 §3.2, Gherkin
     * scenario "Integrity FAIL — dashboard aborta sem prompt").</p>
     *
     * @param data         pre-flight dashboard data; must not be null
     * @param maxChangelog max CHANGELOG lines to show
     * @return evaluation result containing dashboard text and disposition
     */
    public static PreflightResult evaluate(DashboardData data,
                                           int maxChangelog) {
        Objects.requireNonNull(data, "data");
        String dashboard = PreflightDashboardRenderer.render(
                data, maxChangelog);
        IntegrityReport report = data.integrityReport();
        if (report.overallStatus() == CheckStatus.FAIL) {
            return PreflightResult.integrityFail(dashboard);
        }
        return PreflightResult.proceed(dashboard);
    }

    /**
     * Evaluates with default changelog truncation.
     */
    public static PreflightResult evaluate(DashboardData data) {
        return evaluate(data,
                PreflightDashboardRenderer.DEFAULT_CHANGELOG_LINES);
    }

    /**
     * Resolves the operator decision into a {@link PreflightResult}.
     *
     * <p>Called by the adapter after the operator answers the prompt.</p>
     *
     * @param decision  operator choice; must not be null
     * @param dashboard previously rendered dashboard text
     * @return result with appropriate exit code and error code
     */
    public static PreflightResult resolve(PreflightDecision decision,
                                          String dashboard) {
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(dashboard, "dashboard");
        return switch (decision) {
            case PROCEED -> PreflightResult.proceed(dashboard);
            case EDIT_VERSION -> PreflightResult.editVersion(dashboard);
            case ABORT -> PreflightResult.abort(dashboard);
        };
    }
}
