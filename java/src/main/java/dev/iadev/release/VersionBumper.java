package dev.iadev.release;

import java.util.Objects;

/**
 * Pure-domain computation of the next {@link SemVer} given a base version and
 * a {@link BumpType}.
 *
 * <p>Bump rules (per story-0039-0001 §3.1):</p>
 * <ul>
 *   <li>{@link BumpType#MAJOR} &rarr; {@code (M+1).0.0}</li>
 *   <li>{@link BumpType#MINOR} &rarr; {@code M.(m+1).0}</li>
 *   <li>{@link BumpType#PATCH} &rarr; {@code M.m.(p+1)}</li>
 *   <li>{@link BumpType#EXPLICIT} &rarr; not handled here; the caller already
 *       has the explicit version and should use {@link SemVer#parse(String)}
 *       directly. Passing {@code EXPLICIT} to {@link #bump} raises
 *       {@link InvalidBumpException.Code#INVALID_BUMP_COMBINATION}.</li>
 * </ul>
 *
 * <p>Pre-release information on the base version is dropped after a bump
 * (the next version is always a stable release by construction). If a
 * downstream workflow needs to produce a pre-release, it must apply the
 * suffix explicitly after {@link #bump} returns.</p>
 */
public final class VersionBumper {

    private VersionBumper() {
        // utility class, not instantiable
    }

    /**
     * Returns {@code base} bumped by {@code type}.
     *
     * @throws NullPointerException if either argument is {@code null}
     * @throws InvalidBumpException with code
     *         {@link InvalidBumpException.Code#INVALID_BUMP_COMBINATION} when
     *         {@code type == EXPLICIT}
     */
    public static SemVer bump(SemVer base, BumpType type) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(type, "type");
        return switch (type) {
            case MAJOR -> new SemVer(base.major() + 1, 0, 0, null);
            case MINOR -> new SemVer(base.major(), base.minor() + 1, 0, null);
            case PATCH -> new SemVer(base.major(), base.minor(), base.patch() + 1, null);
            case EXPLICIT -> throw new InvalidBumpException(
                    InvalidBumpException.Code.INVALID_BUMP_COMBINATION,
                    "EXPLICIT bumps must be constructed via SemVer.parse(), not VersionBumper.bump()");
        };
    }
}
