package dev.iadev.application.lifecycle;

import java.nio.file.Path;
import java.util.List;

/**
 * Scans a skills directory (or any tree of markdown / Java
 * source) for lifecycle-integrity violations as defined in
 * Rule 22.
 *
 * <p>This class is the Layer-0 skeleton delivered by
 * story-0046-0001. Its public contract (method signature,
 * {@link Violation} record shape) is STABLE — story-0046-0007
 * adds the real detection logic without breaking the API.
 * Until then, {@link #scan(Path)} returns {@code List.of()}
 * so downstream tests can depend on the runner without
 * spurious failures.</p>
 */
public final class LifecycleAuditRunner {

    /**
     * Scans the given root directory and returns a list of
     * violations. The skeleton returns an empty list — real
     * detection is implemented in story-0046-0007.
     *
     * @param skillsRoot directory to scan (typically
     *     {@code java/src/main/resources/targets/claude/skills})
     * @return the violations; empty in the skeleton
     */
    public List<Violation> scan(Path skillsRoot) {
        // Contract stub: production detection arrives in
        // story-0046-0007 (LifecycleIntegrityAuditTest).
        if (skillsRoot == null) {
            return List.of();
        }
        return List.of();
    }
}
