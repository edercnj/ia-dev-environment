package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;

import java.nio.file.Path;
import java.util.Set;

/**
 * Immutable options for pipeline execution.
 *
 * <p>Controls pipeline behavior such as dry-run mode,
 * force overwrite, verbose logging, constitution overwrite,
 * custom resources directory, and platform filtering.</p>
 *
 * <p>The {@code platforms} field enables selective
 * generation for specific AI platforms. An empty set
 * means no filter (all assemblers run). When all
 * user-selectable platforms are specified, it is
 * equivalent to no filter (RULE-001).</p>
 *
 * @param dryRun                 if true, generates files
 *     but does not write to the final destination
 * @param force                  if true, allows overwriting
 *     existing artifacts
 * @param verbose                if true, emits detailed log
 *     output
 * @param overwriteConstitution  if true, regenerates
 *     CONSTITUTION.md even when it already exists
 * @param resourcesDir           custom resources/templates
 *     directory, or null for auto-resolution
 * @param platforms              the target platforms for
 *     filtering; empty means all (no filter)
 */
public record PipelineOptions(
        boolean dryRun,
        boolean force,
        boolean verbose,
        boolean overwriteConstitution,
        Path resourcesDir,
        Set<Platform> platforms) {

    /**
     * Canonical constructor that ensures platforms is
     * immutable and never null.
     */
    public PipelineOptions {
        platforms = platforms == null
                ? Set.of()
                : Set.copyOf(platforms);
    }

    /**
     * Backward-compatible 5-arg constructor without
     * platforms (defaults to empty = no filter).
     *
     * @param dryRun                 dry-run mode flag
     * @param force                  force overwrite flag
     * @param verbose                verbose output flag
     * @param overwriteConstitution  constitution overwrite
     * @param resourcesDir           custom resources dir
     */
    public PipelineOptions(
            boolean dryRun,
            boolean force,
            boolean verbose,
            boolean overwriteConstitution,
            Path resourcesDir) {
        this(dryRun, force, verbose,
                overwriteConstitution,
                resourcesDir, Set.of());
    }

    /**
     * Backward-compatible 4-arg constructor without
     * overwriteConstitution or platforms.
     *
     * @param dryRun       dry-run mode flag
     * @param force        force overwrite flag
     * @param verbose      verbose output flag
     * @param resourcesDir custom resources directory
     */
    public PipelineOptions(
            boolean dryRun,
            boolean force,
            boolean verbose,
            Path resourcesDir) {
        this(dryRun, force, verbose, false,
                resourcesDir, Set.of());
    }

    /**
     * Creates default pipeline options with all flags
     * disabled and no platform filter.
     *
     * @return a PipelineOptions with all flags false,
     *         resourcesDir null, and empty platforms
     */
    public static PipelineOptions defaults() {
        return new PipelineOptions(
                false, false, false, false,
                null, Set.of());
    }
}
