package dev.iadev.assembler;

import dev.iadev.application.assembler.ScriptsAssembler;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ScriptsAssembler}.
 *
 * <p>Validates that the assembler copies all 5 audit scripts
 * to the output scripts/ directory, sets executable
 * permissions, and returns the correct list of generated paths.</p>
 */
@DisplayName("ScriptsAssemblerTest")
class ScriptsAssemblerTest {

    @TempDir
    Path tempDir;

    private final ScriptsAssembler assembler =
            new ScriptsAssembler();

    private final ProjectConfig config =
            ConfigProfiles.getStack("java-spring");

    private final TemplateEngine engine =
            new TemplateEngine();

    @Test
    @DisplayName("assemble returns paths for all 5 audit scripts")
    void assemble_returnsPathsForAllAuditScripts() {
        List<String> generated =
                assembler.assemble(config, engine, tempDir);

        assertThat(generated)
                .as("Should generate 5 audit script paths")
                .hasSize(ScriptsAssembler.AUDIT_SCRIPTS.size());
    }

    @Test
    @DisplayName("assemble creates scripts/ directory")
    void assemble_createsScriptsDirectory() {
        assembler.assemble(config, engine, tempDir);

        Path scriptsDir =
                tempDir.resolve("scripts");
        assertThat(scriptsDir)
                .as("scripts must be created")
                .exists()
                .isDirectory();
    }

    @Test
    @DisplayName("assemble copies each audit script to output dir")
    void assemble_copiesEachAuditScript() {
        assembler.assemble(config, engine, tempDir);

        Path scriptsDir =
                tempDir.resolve("scripts");
        for (String scriptName :
                ScriptsAssembler.AUDIT_SCRIPTS) {
            assertThat(scriptsDir.resolve(scriptName))
                    .as("Script %s must be copied",
                            scriptName)
                    .exists()
                    .isRegularFile();
        }
    }

    @Test
    @DisplayName("assemble sets owner execute permission (POSIX)")
    void assemble_setsExecutePermission()
            throws IOException {
        assembler.assemble(config, engine, tempDir);

        Path scriptsDir =
                tempDir.resolve("scripts");
        String firstName =
                ScriptsAssembler.AUDIT_SCRIPTS.get(0);
        Path script = scriptsDir.resolve(firstName);

        try {
            Set<PosixFilePermission> perms =
                    Files.getPosixFilePermissions(script);
            assertThat(perms)
                    .as("Script must be owner-executable")
                    .contains(
                            PosixFilePermission.OWNER_EXECUTE);
        } catch (UnsupportedOperationException e) {
            // Non-POSIX filesystem — skip permission check
        }
    }

    @Test
    @DisplayName("AUDIT_SCRIPTS list contains exactly 5 scripts")
    void auditScripts_constantHasExpectedSize() {
        assertThat(ScriptsAssembler.AUDIT_SCRIPTS)
                .hasSize(5)
                .contains(
                        "audit-flow-version.sh",
                        "audit-epic-branches.sh",
                        "audit-skill-visibility.sh",
                        "audit-model-selection.sh",
                        "audit-execution-integrity.sh");
    }

    @Test
    @DisplayName("assemble is idempotent on repeated invocations")
    void assemble_isIdempotent() {
        List<String> first =
                assembler.assemble(config, engine, tempDir);
        List<String> second =
                assembler.assemble(config, engine, tempDir);

        assertThat(second)
                .as("Second invocation should also return 5 paths")
                .hasSize(first.size());
    }
}
