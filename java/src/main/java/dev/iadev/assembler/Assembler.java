package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.List;

/**
 * Uniform assembler contract per RULE-004.
 *
 * <p>Every assembler in the pipeline MUST implement this
 * interface. The {@link #assemble} method receives the project
 * configuration, the template engine, and the output directory,
 * and returns the list of absolute paths of generated files.</p>
 *
 * <p>Assemblers MUST NOT have side effects beyond writing files
 * to the output directory. Each assembler is responsible for
 * creating its own subdirectories as needed.</p>
 *
 * <p>Assemblers that need to report warnings SHOULD override
 * {@link #assembleWithResult} to return an
 * {@link AssemblerResult} containing both files and warnings.
 * The default implementation wraps {@link #assemble} with an
 * empty warnings list.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler rules = new RulesAssembler();
 * List<String> files = rules.assemble(config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see AssemblerPipeline
 * @see AssemblerDescriptor
 * @see AssemblerResult
 */
@FunctionalInterface
public interface Assembler {

    /**
     * Generates artifacts for this assembler's domain.
     *
     * @param config    the project configuration
     * @param engine    the template rendering engine
     * @param outputDir the target output directory
     * @return list of paths of generated files
     */
    List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir);

    /**
     * Generates artifacts and returns a structured result
     * containing both files and warnings.
     *
     * <p>The default implementation delegates to
     * {@link #assemble} and wraps the result with an empty
     * warnings list. Override to propagate warnings.</p>
     *
     * @param config    the project configuration
     * @param engine    the template rendering engine
     * @param outputDir the target output directory
     * @return result with generated files and warnings
     */
    default AssemblerResult assembleWithResult(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        List<String> files =
                assemble(config, engine, outputDir);
        return AssemblerResult.of(files, List.of());
    }
}
