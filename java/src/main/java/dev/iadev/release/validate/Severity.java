package dev.iadev.release.validate;

/**
 * Severity of an individual validation check outcome.
 *
 * <p>Order is significant: {@link #FAIL} &lt; {@link #WARN}
 * &lt; {@link #PASS}. Aggregated results are sorted with FAIL
 * first so the first failure code is deterministic.</p>
 */
public enum Severity {
    FAIL,
    WARN,
    PASS
}
