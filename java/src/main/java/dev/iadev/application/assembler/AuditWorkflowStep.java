package dev.iadev.application.assembler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Generates the audit CI workflow artifact
 * ({@code .github/workflows/audit.yml}).
 *
 * <p>The audit workflow runs the 5 CI audit scripts on every
 * PR to {@code develop} or {@code epic/**} branches, enforcing
 * the governance gates defined in Rule 26 (Audit Gate Lifecycle)
 * and EPIC-0058. Always generated for CLAUDE_CODE platform.</p>
 *
 * <p>Introduced by story-0058-0008 (EPIC-0058). The template
 * lives at {@code shared/cicd-templates/audit-workflow/audit.yml.njk}.
 * Scripts referenced in the generated workflow are produced by
 * {@link ScriptsAssembler} and placed in {@code .claude/scripts/}.</p>
 *
 * @see CicdAssembler
 * @see ScriptsAssembler
 */
final class AuditWorkflowStep {

    private static final String CICD_TEMPLATES =
            "shared/cicd-templates";
    private static final String AUDIT_TEMPLATE =
            "audit-workflow/audit.yml.njk";

    /**
     * Generates the audit workflow file.
     *
     * @param cicdCtx the CI/CD context
     * @return the generation result
     */
    CicdResult assemble(CicdContext cicdCtx) {
        Path dest = cicdCtx.outputDir()
                .resolve(".github")
                .resolve("workflows")
                .resolve("audit.yml");
        Optional<String> err =
                renderAndWrite(cicdCtx, dest);
        if (err.isEmpty()) {
            return new CicdResult(
                    List.of(dest.toString()),
                    List.of());
        }
        return new CicdResult(
                List.of(),
                List.of(err.orElseThrow()));
    }

    private Optional<String> renderAndWrite(
            CicdContext cicdCtx, Path destPath) {
        try {
            String content = cicdCtx.engine().render(
                    CICD_TEMPLATES + "/"
                            + AUDIT_TEMPLATE,
                    cicdCtx.ctx());
            CopyHelpers.ensureDirectory(
                    destPath.getParent());
            Files.writeString(destPath, content,
                    java.nio.charset.StandardCharsets.UTF_8);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(
                    "audit.yml generation failed: "
                            + e.getMessage());
        }
    }
}
