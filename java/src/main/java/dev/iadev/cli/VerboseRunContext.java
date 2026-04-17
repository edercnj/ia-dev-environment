package dev.iadev.cli;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.PipelineOptions;

import java.util.List;

/**
 * Cohesive carrier for the pipeline run configuration used
 * by {@link VerbosePipelineRunner#runVerbose}.
 *
 * <p>Groups pipeline options with the filtered assembler set
 * being executed and the complete descriptor set used for
 * header rendering. Introduced to reduce
 * {@code runVerbose} to ≤ 4 parameters (RULE-003).</p>
 *
 * @param options    pipeline options (platforms, dry-run)
 * @param assemblers filtered assembler descriptors (to run)
 * @param all        complete assembler descriptors (universe)
 */
record VerboseRunContext(
        PipelineOptions options,
        List<AssemblerDescriptor> assemblers,
        List<AssemblerDescriptor> all) {
}
