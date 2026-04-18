package dev.iadev.release;

import java.util.Objects;
import java.util.Optional;

/**
 * Enumerates how the next version was derived.
 *
 * <ul>
 *   <li>{@link #MAJOR} — at least one commit carried {@code !} or
 *       {@code BREAKING CHANGE:}</li>
 *   <li>{@link #MINOR} — no breaking signal, at least one {@code feat:}</li>
 *   <li>{@link #PATCH} — no breaking, no feat, at least one {@code fix:} or
 *       {@code perf:}</li>
 *   <li>{@link #EXPLICIT} — operator passed {@code --version X.Y.Z} on the CLI
 *       (auto-detection was bypassed)</li>
 * </ul>
 *
 * <p>{@link #from(CommitCounts)} returns {@link Optional#empty()} when no commit
 * justifies a bump (all commits were ignored types). Callers MUST map the empty
 * result to the {@code VERSION_NO_BUMP_SIGNAL} error code before aborting.</p>
 */
public enum BumpType {
    MAJOR,
    MINOR,
    PATCH,
    EXPLICIT;

    /**
     * Selects the highest-precedence bump implied by {@code counts}.
     *
     * @return {@link #MAJOR} if {@code breaking > 0}, {@link #MINOR} if
     *         {@code feat > 0}, {@link #PATCH} if {@code fix + perf > 0},
     *         {@link Optional#empty()} otherwise.
     */
    public static Optional<BumpType> from(CommitCounts counts) {
        Objects.requireNonNull(counts, "counts");
        if (counts.breaking() > 0) {
            return Optional.of(MAJOR);
        }
        if (counts.feat() > 0) {
            return Optional.of(MINOR);
        }
        if (counts.fix() + counts.perf() > 0) {
            return Optional.of(PATCH);
        }
        return Optional.empty();
    }
}
