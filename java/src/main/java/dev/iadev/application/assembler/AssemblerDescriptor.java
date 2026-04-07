package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;

import java.util.Set;

/**
 * Pairs an assembler with its display name, target directory,
 * and platform metadata.
 *
 * <p>Used by {@link AssemblerPipeline} to maintain the ordered
 * list of assemblers with their metadata. Each descriptor
 * identifies which assembler to run, where its output should
 * be written, and which AI platform(s) it belongs to.</p>
 *
 * <p>The {@code platforms} field enables platform-based
 * filtering without altering existing execution logic.</p>
 *
 * @param name      the display name (e.g., "RulesAssembler")
 * @param target    the logical output target directory
 * @param platforms the AI platforms this assembler belongs to
 *                  (immutable, at least one element)
 * @param assembler the assembler implementation
 * @see AssemblerTarget
 * @see Platform
 */
public record AssemblerDescriptor(
        String name,
        AssemblerTarget target,
        Set<Platform> platforms,
        Assembler assembler) {

    /**
     * Canonical constructor that ensures platforms is
     * immutable.
     */
    public AssemblerDescriptor {
        if (platforms == null || platforms.isEmpty()) {
            throw new IllegalArgumentException(
                    "platforms must not be null or empty");
        }
        platforms = Set.copyOf(platforms);
    }
}
