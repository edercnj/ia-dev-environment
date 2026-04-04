package dev.iadev.assembler;

import java.nio.file.Path;

/**
 * Logical target directories for assembler output.
 *
 * <p>Maps each assembler's logical target to the physical
 * subdirectory within the output directory. Used by
 * {@link AssemblerPipeline} to route each assembler's output
 * to the correct location.</p>
 *
 * <p>Physical directory mapping:
 * <table>
 * <tr><th>Target</th><th>Physical Directory</th></tr>
 * <tr><td>ROOT</td><td>{@code outputDir}</td></tr>
 * <tr><td>CLAUDE</td><td>{@code outputDir/.claude}</td></tr>
 * <tr><td>GITHUB</td><td>{@code outputDir/.github}</td></tr>
 * <tr><td>CODEX</td><td>{@code outputDir/.codex}</td></tr>
 * <tr><td>CODEX_AGENTS</td><td>{@code outputDir/.agents}</td></tr>
 * <tr><td>DOCS</td><td>{@code outputDir/docs}</td></tr>
 * </table>
 *
 * @see AssemblerDescriptor
 */
public enum AssemblerTarget {

    /** Output root directory. */
    ROOT(""),

    /** {@code .claude/} subdirectory. */
    CLAUDE(".claude"),

    /** {@code .github/} subdirectory. */
    GITHUB(".github"),

    /** {@code .codex/} subdirectory. */
    CODEX(".codex"),

    /** {@code .agents/} subdirectory. */
    CODEX_AGENTS(".agents"),

    /** {@code docs/} subdirectory. */
    DOCS("docs");

    private final String subdir;

    AssemblerTarget(String subdir) {
        this.subdir = subdir;
    }

    /**
     * Resolves this target to a physical path relative to
     * the given base directory.
     *
     * @param baseDir the base output directory
     * @return the resolved physical path
     */
    public Path resolve(Path baseDir) {
        if (subdir.isEmpty()) {
            return baseDir;
        }
        return baseDir.resolve(subdir);
    }
}
