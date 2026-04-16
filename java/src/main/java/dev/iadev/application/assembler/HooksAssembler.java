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
 * from the {@code targets/claude/hooks/{key}/} resources directory
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
            "targets/claude/hooks";

    /**
     * Telemetry scripts copied when
     * {@link ProjectConfig#telemetryEnabled()} is {@code true}
     * (story-0040-0004). Sourced from
     * {@code targets/claude/hooks/telemetry-*.sh}.
     */
    public static final List<String> TELEMETRY_SCRIPTS = List.of(
            "telemetry-emit.sh",
            "telemetry-lib.sh",
            "telemetry-session.sh",
            "telemetry-pretool.sh",
            "telemetry-posttool.sh",
            "telemetry-subagent.sh",
            "telemetry-stop.sh");

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
        List<String> written = new java.util.ArrayList<>();
        written.addAll(copyPostCompileHook(
                config, outputDir));
        if (config.telemetryEnabled()) {
            written.addAll(copyTelemetryScripts(outputDir));
        }
        return List.copyOf(written);
    }

    private List<String> copyPostCompileHook(
            ProjectConfig config, Path outputDir) {
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

    private List<String> copyTelemetryScripts(
            Path outputDir) {
        Path hooksDir = outputDir.resolve(HOOKS_DIR);
        CopyHelpers.ensureDirectory(hooksDir);
        List<String> copied = new java.util.ArrayList<>();
        for (String name : TELEMETRY_SCRIPTS) {
            Path src = resourcesDir.resolve(
                    HOOKS_TEMPLATES_DIR + "/" + name);
            if (!Files.exists(src)) {
                throw new UncheckedIOException(
                        new IOException(
                                "Telemetry hook source not"
                                        + " found: " + src));
            }
            Path dest = hooksDir.resolve(name);
            try {
                Files.copy(src, dest,
                        StandardCopyOption.REPLACE_EXISTING);
                makeExecutable(dest);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Failed to copy telemetry hook: %s"
                                .formatted(src), e);
            }
            copied.add(dest.toString());
        }
        return copied;
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
                .resolveResourceDir("shared")
                .getParent();
    }
}
