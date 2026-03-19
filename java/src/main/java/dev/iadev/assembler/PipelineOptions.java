package dev.iadev.assembler;

import java.nio.file.Path;

/**
 * Immutable options for pipeline execution.
 *
 * <p>Controls pipeline behavior such as dry-run mode,
 * force overwrite, verbose logging, and custom resources
 * directory.</p>
 *
 * @param dryRun       if true, generates files but does not
 *                     write to the final destination
 * @param force        if true, allows overwriting existing
 *                     artifacts
 * @param verbose      if true, emits detailed log output
 * @param resourcesDir custom resources/templates directory,
 *                     or null for auto-resolution
 */
public record PipelineOptions(
        boolean dryRun,
        boolean force,
        boolean verbose,
        Path resourcesDir) {

    /**
     * Creates default pipeline options with all flags disabled.
     *
     * @return a PipelineOptions with dryRun=false, force=false,
     *         verbose=false, resourcesDir=null
     */
    public static PipelineOptions defaults() {
        return new PipelineOptions(
                false, false, false, null);
    }
}
