package dev.iadev.release;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port (Rule 04) for reading git tag + commit-range state.
 *
 * <p>Implemented by {@link GitTagReader}; kept in the domain package so that
 * story-0039-0001 consumers depend on the abstraction, not the concrete
 * {@code ProcessBuilder}-based adapter.</p>
 */
public interface TagReader {

    /**
     * Returns the most recent git tag matching {@code v*}, or
     * {@link Optional#empty()} when the repository has no such tags (e.g.
     * first release).
     */
    Optional<String> lastTag();

    /**
     * Returns the commit payloads in {@code <fromRef>..HEAD} formatted as
     * {@code %s%n%b} (subject followed by body). Each list element is one
     * commit. When {@code fromRef} is {@link Optional#empty()} the range is
     * all commits from the repository root ({@code HEAD}).
     */
    List<String> commitsSince(Optional<String> fromRef);
}
