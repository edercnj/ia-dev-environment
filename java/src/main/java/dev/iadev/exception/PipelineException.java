package dev.iadev.exception;

/**
 * Thrown when an assembler fails during artifact generation in the pipeline.
 *
 * <p>Carries the name of the assembler that failed, enabling targeted
 * debugging across the 23 assemblers in the pipeline.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     assembler.assemble(config, engine, outputDir);
 * } catch (IOException e) {
 *     throw new PipelineException(
 *         "Pipeline failed at RulesAssembler",
 *         "RulesAssembler", e);
 * }
 * }</pre>
 */
public class PipelineException extends RuntimeException {

    private final String assemblerName;

    /**
     * Creates a pipeline exception identifying the failed assembler.
     *
     * @param message       description of the pipeline failure
     * @param assemblerName name of the assembler that failed
     * @param cause         the original exception from the assembler
     */
    public PipelineException(
            String message, String assemblerName, Throwable cause) {
        super(message, cause);
        this.assemblerName = assemblerName;
    }

    /**
     * Returns the name of the assembler that failed.
     *
     * @return the assembler name
     */
    public String getAssemblerName() {
        return assemblerName;
    }

    @Override
    public String toString() {
        return "PipelineException{message='%s', assemblerName='%s'}"
                .formatted(getMessage(), assemblerName);
    }
}
