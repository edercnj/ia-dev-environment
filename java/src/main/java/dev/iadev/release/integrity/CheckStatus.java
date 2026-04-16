package dev.iadev.release.integrity;

/**
 * Status of an individual integrity check.
 *
 * <p>PASS and WARN do not abort the release; FAIL does.</p>
 */
public enum CheckStatus {
    PASS,
    WARN,
    FAIL
}
