package dev.iadev.application.assembler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates CI workflow artifacts ({@code .github/workflows/ci.yml}).
 *
 * <p>CI workflow is always generated regardless of project
 * configuration flags.</p>
 *
 * @see CicdAssembler
 */
final class CiWorkflowAssembler {

    private static final String CICD_TEMPLATES =
            "cicd-templates";
    private static final String CI_TEMPLATE =
            "ci-workflow/ci.yml.njk";

    /**
     * Generates the CI workflow file.
     *
     * @param cicdCtx the CI/CD context
     * @return the generation result
     */
    CicdResult assemble(CicdContext cicdCtx) {
        Path dest = cicdCtx.outputDir()
                .resolve(".github")
                .resolve("workflows")
                .resolve("ci.yml");
        Optional<String> err = renderAndWrite(
                cicdCtx, CI_TEMPLATE, dest);
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
            CicdContext cicdCtx,
            String templateRelPath,
            Path destPath) {
        try {
            String content = cicdCtx.engine().render(
                    CICD_TEMPLATES + "/"
                            + templateRelPath,
                    cicdCtx.ctx());
            CopyHelpers.ensureDirectory(
                    destPath.getParent());
            Files.writeString(
                    destPath, content,
                    StandardCharsets.UTF_8);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(
                    "Failed to render %s: %s"
                            .formatted(templateRelPath,
                                    e.getMessage()));
        }
    }
}
