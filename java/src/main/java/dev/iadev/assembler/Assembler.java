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
 * <p>Example usage:
 * <pre>{@code
 * Assembler rules = new RulesAssembler();
 * List<String> files = rules.assemble(config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see AssemblerPipeline
 * @see AssemblerDescriptor
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
}
