package dev.iadev.release.dryrun;

import java.util.List;

/**
 * Port providing the ordered list of release phases that
 * will be simulated by
 * {@link DryRunInteractiveExecutor}.
 *
 * <p>The canonical catalog has 13 phases (story-0039-0013
 * §3.1). Test doubles may return a shorter list for
 * focused scenarios.
 */
@FunctionalInterface
public interface PhaseCatalogPort {

    /**
     * Returns the ordered catalog of phases.
     *
     * @return immutable ordered list of phase descriptors
     */
    List<PhaseDescriptor> phases();
}
