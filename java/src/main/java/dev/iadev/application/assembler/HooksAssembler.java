package dev.iadev.application.assembler;

import dev.iadev.domain.stack.StackMapping;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

/**
 * Assembles {@code .claude/hooks/} with post-compile hook
 * scripts for compiled languages.
 *
 * <p>This is the sixth assembler in the pipeline (position 6
 * of 23 per RULE-005). It copies pre-written hook scripts
 * from the {@code hooks-templates/{key}/} resources directory
 * to the output {@code hooks/} directory.</p>
 *
 * <p>Hook scripts are copied verbatim from templates — no
 * placeholder replacement is performed. The {@code engine}
 * parameter is accepted for API uniformity but is not used.
 * After copying, the script is marked as executable.</p>
 *
 * <p>The hook template key is resolved via
 * {@link StackMapping#getHookTemplateKey}. If the key is
 * empty (e.g., for Python), no hook is generated.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler hooks = new HooksAssembler();
 * List<String> files = hooks.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see StackMapping#getHookTemplateKey
 */
public final class HooksAssembler implements Assembler {

    private static final String HOOKS_DIR = "hooks";
    private static final String HOOK_FILENAME =
            "post-compile-check.sh";
    private static final String HOOKS_TEMPLATES_DIR =
            "hooks-templates";

    private final Path resourcesDir;

    /**
     * Creates a HooksAssembler using classpath resources.
     */
    public HooksAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a HooksAssembler with an explicit resources
     * directory.
     *
     * @param resourcesDir the base resources directory
     */
    public HooksAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Copies the post-compile hook script for the project's
     * language/build-tool combination. Returns an empty list if
     * no hook template exists for the stack.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        String key = StackMapping.getHookTemplateKey(
                config.language().name(),
                config.framework().buildTool());
        if (key.isEmpty()) {
            return List.of();
        }
        Path hookSrc = resourcesDir.resolve(
                HOOKS_TEMPLATES_DIR + "/" + key + "/"
                        + HOOK_FILENAME);
        if (!Files.exists(hookSrc)) {
            return List.of();
        }
        return copyHook(hookSrc, outputDir);
    }

    private List<String> copyHook(
            Path hookSrc, Path outputDir) {
        Path hooksDir = outputDir.resolve(HOOKS_DIR);
        CopyHelpers.ensureDirectory(hooksDir);
        Path dest = hooksDir.resolve(HOOK_FILENAME);
        try {
            Files.copy(hookSrc, dest,
                    StandardCopyOption.REPLACE_EXISTING);
            makeExecutable(dest);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to copy hook script: %s"
                            .formatted(hookSrc), e);
        }
        return List.of(dest.toString());
    }

    /**
     * Sets the executable permission on the given file.
     *
     * <p>On POSIX systems, adds owner/group/others execute
     * permissions. On non-POSIX systems (e.g., Windows),
     * this is a no-op since POSIX permissions are not
     * supported.</p>
     *
     * @param file the file to make executable
     */
    static void makeExecutable(Path file) {
        try {
            Set<PosixFilePermission> perms =
                    Files.getPosixFilePermissions(file);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(file, perms);
        } catch (UnsupportedOperationException e) {
            // Non-POSIX filesystem (e.g., Windows)
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to set executable permission: %s"
                            .formatted(file), e);
        }
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(HOOKS_TEMPLATES_DIR);
    }
}
