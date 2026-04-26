package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Assembles {@code .claude/scripts/} with CI audit scripts.
 *
 * <p>This is the 23rd assembler in the pipeline. It copies
 * audit scripts from the classpath resource directory
 * {@code targets/claude/scripts/} to the output
 * {@code .claude/scripts/} directory, setting POSIX execute
 * permissions on POSIX-capable filesystems.</p>
 *
 * <p>Source-of-truth: {@code java/src/main/resources/targets/claude/scripts/}
 * Output: {@code <project>/.claude/scripts/}</p>
 *
 * <p>Scripts are copied verbatim — no placeholder replacement.
 * This assembler is introduced by EPIC-0058 (story-0058-0006)
 * to achieve generation parity (RULE-003): projects generated
 * by {@code ia-dev-env} automatically inherit the governance
 * CI gates, matching the parity already achieved for hooks.</p>
 *
 * @see HooksAssembler
 * @see Assembler
 */
public final class ScriptsAssembler implements Assembler {

    private static final Logger LOG =
            Logger.getLogger(ScriptsAssembler.class.getName());

    static final String SCRIPTS_OUTPUT_DIR = ".claude/scripts";
    static final String SCRIPTS_CLASSPATH_PREFIX =
            "targets/claude/scripts/";

    private static final Set<PosixFilePermission>
            EXECUTABLE_PERMS = Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE);

    /**
     * The canonical list of audit scripts bundled in the
     * source-of-truth directory. Ordered alphabetically.
     * This list is the authoritative inventory; golden tests
     * assert all 5 are present in generated output.
     */
    public static final List<String> AUDIT_SCRIPTS = List.of(
            "audit-epic-branches.sh",
            "audit-execution-integrity.sh",
            "audit-flow-version.sh",
            "audit-model-selection.sh",
            "audit-skill-visibility.sh");

    /**
     * {@inheritDoc}
     *
     * <p>Copies all {@code audit-*.sh} scripts from the classpath
     * prefix {@value #SCRIPTS_CLASSPATH_PREFIX} to the output
     * {@code .claude/scripts/} directory. Marks each file as
     * executable on POSIX-capable filesystems. Silently skips
     * the permission step on Windows or other non-POSIX systems.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path scriptsDir =
                outputDir.resolve(SCRIPTS_OUTPUT_DIR);
        try {
            Files.createDirectories(scriptsDir);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to create .claude/scripts dir", e);
        }

        List<String> generated = new ArrayList<>();
        for (String scriptName : AUDIT_SCRIPTS) {
            String resourcePath =
                    SCRIPTS_CLASSPATH_PREFIX + scriptName;
            URL resource = getClass().getClassLoader()
                    .getResource(resourcePath);
            if (resource == null) {
                LOG.warning(() ->
                        "Script resource not found: "
                                + resourcePath);
                continue;
            }

            Path target = scriptsDir.resolve(scriptName);
            try (InputStream is = resource.openStream()) {
                Files.copy(is, target,
                        StandardCopyOption.REPLACE_EXISTING);
                setExecutable(target);
                generated.add(target.toString());
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Failed to copy script: "
                                + scriptName, e);
            }
        }

        return List.copyOf(generated);
    }

    private void setExecutable(Path file) {
        try {
            Files.setPosixFilePermissions(
                    file, EXECUTABLE_PERMS);
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystem (e.g., Windows NTFS)
        } catch (IOException e) {
            LOG.warning(() ->
                    "Failed to set executable bit on "
                            + file + ": " + e.getMessage());
        }
    }
}
