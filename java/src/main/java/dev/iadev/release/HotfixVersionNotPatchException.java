package dev.iadev.release;

import java.util.Objects;

/**
 * Raised when an explicit {@code --version} override supplied
 * to a hotfix flow is not a valid PATCH bump over the current
 * version (same MAJOR.MINOR, PATCH + 1). Story-0039-0014 §5.4
 * example: current {@code 3.1.0}, requested {@code 3.2.0} →
 * rejected.
 *
 * <p>Error code: {@code HOTFIX_VERSION_NOT_PATCH} — exit 1.</p>
 */
public final class HotfixVersionNotPatchException
        extends RuntimeException {

    /** Standardised error code surfaced to the operator. */
    public static final String CODE =
            "HOTFIX_VERSION_NOT_PATCH";

    private static final long serialVersionUID = 1L;

    private final SemVer current;
    private final SemVer requested;

    public HotfixVersionNotPatchException(
            SemVer current, SemVer requested) {
        super(buildMessage(current, requested));
        this.current = Objects.requireNonNull(
                current, "current");
        this.requested = Objects.requireNonNull(
                requested, "requested");
    }

    public SemVer current() {
        return current;
    }

    public SemVer requested() {
        return requested;
    }

    /** Stable error code. */
    public String code() {
        return CODE;
    }

    private static String buildMessage(
            SemVer current, SemVer requested) {
        return String.format(
                "Hotfix override must be PATCH only "
                        + "(current=%s, requested=%s).",
                current, requested);
    }
}
