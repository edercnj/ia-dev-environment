package dev.iadev.application.assembler;

import java.nio.file.Path;

/**
 * Immutable options for pipeline execution.
 *
 * <p>Controls pipeline behavior such as dry-run mode,
 * force overwrite, verbose logging, constitution overwrite,
 * and custom resources directory.</p>
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
 */
public record PipelineOptions(
        boolean dryRun,
        boolean force,
        boolean verbose,
        boolean overwriteConstitution,
        Path resourcesDir) {

    /**
     * Backward-compatible constructor without
     * overwriteConstitution (defaults to false).
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
        this(dryRun, force, verbose, false, resourcesDir);
    }

    /**
     * Creates default pipeline options with all flags
     * disabled.
     *
     * @return a PipelineOptions with all flags false and
     *         resourcesDir null
     */
    public static PipelineOptions defaults() {
        return new PipelineOptions(
                false, false, false, false, null);
    }
}
