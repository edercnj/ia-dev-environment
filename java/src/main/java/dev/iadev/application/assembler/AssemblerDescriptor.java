package dev.iadev.application.assembler;

/**
 * Pairs an assembler with its display name and target directory.
 *
 * <p>Used by {@link AssemblerPipeline} to maintain the ordered
 * list of assemblers with their metadata. Each descriptor
 * identifies which assembler to run and where its output should
 * be written.</p>
 *
 * @param name      the display name (e.g., "RulesAssembler")
 * @param target    the logical output target directory
 * @param assembler the assembler implementation
 * @see AssemblerTarget
 */
public record AssemblerDescriptor(
        String name,
        AssemblerTarget target,
        Assembler assembler) {
}
