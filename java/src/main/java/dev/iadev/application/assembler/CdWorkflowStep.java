package dev.iadev.application.assembler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generates CD workflow artifacts (release, deploy-staging,
 * deploy-production, rollback) in
 * {@code .github/workflows/}.
 *
 * <p>Release workflow is always generated. Deploy and
 * rollback workflows are conditional on
 * {@code container == "docker"}.</p>
 *
 * @see CicdAssembler
 * @see CiWorkflowStep
 */
final class CdWorkflowStep {

    private static final String CICD_TEMPLATES =
            "shared/cicd-templates";
    private static final String CD_TEMPLATE_DIR =
            "cd-workflow";
    private static final String DOCKER_CONDITION =
            "docker";

    private static final String RELEASE_TEMPLATE =
            "release.yml.njk";
    private static final String STAGING_TEMPLATE =
            "deploy-staging.yml.njk";
    private static final String PRODUCTION_TEMPLATE =
            "deploy-production.yml.njk";
    private static final String ROLLBACK_TEMPLATE =
            "rollback.yml.njk";

    /**
     * Generates CD workflow files. Release is always
     * generated. Deploy and rollback are conditional
     * on container configuration.
     *
     * @param cicdCtx the CI/CD context
     * @return the generation result
     */
    CicdResult assemble(CicdContext cicdCtx) {
        List<String> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        renderRelease(cicdCtx, files, warnings);
        renderConditionalWorkflows(
                cicdCtx, files, warnings);

        return new CicdResult(files, warnings);
    }

    private void renderRelease(
            CicdContext cicdCtx,
            List<String> files,
            List<String> warnings) {
        Path dest = workflowPath(
                cicdCtx, "release.yml");
        Optional<String> err = renderAndWrite(
                cicdCtx, RELEASE_TEMPLATE, dest);
        if (err.isEmpty()) {
            files.add(dest.toString());
        } else {
            warnings.add(err.orElseThrow());
        }
    }

    private void renderConditionalWorkflows(
            CicdContext cicdCtx,
            List<String> files,
            List<String> warnings) {
        if (!isDockerEnabled(cicdCtx)) {
            return;
        }
        renderWorkflow(cicdCtx, STAGING_TEMPLATE,
                "deploy-staging.yml", files, warnings);
        renderWorkflow(cicdCtx, PRODUCTION_TEMPLATE,
                "deploy-production.yml", files, warnings);
        renderWorkflow(cicdCtx, ROLLBACK_TEMPLATE,
                "rollback.yml", files, warnings);
    }

    private void renderWorkflow(
            CicdContext cicdCtx,
            String template,
            String filename,
            List<String> files,
            List<String> warnings) {
        Path dest = workflowPath(cicdCtx, filename);
        Optional<String> err = renderAndWrite(
                cicdCtx, template, dest);
        if (err.isEmpty()) {
            files.add(dest.toString());
        } else {
            warnings.add(err.orElseThrow());
        }
    }

    private boolean isDockerEnabled(
            CicdContext cicdCtx) {
        return DOCKER_CONDITION.equals(
                cicdCtx.config().infrastructure()
                        .container());
    }

    private Path workflowPath(
            CicdContext cicdCtx, String filename) {
        return cicdCtx.outputDir()
                .resolve(".github")
                .resolve("workflows")
                .resolve(filename);
    }

    private Optional<String> renderAndWrite(
            CicdContext cicdCtx,
            String templateName,
            Path destPath) {
        try {
            String templatePath =
                    CICD_TEMPLATES + "/"
                            + CD_TEMPLATE_DIR + "/"
                            + templateName;
            String content = cicdCtx.engine().render(
                    templatePath, cicdCtx.ctx());
            CopyHelpers.ensureDirectory(
                    destPath.getParent());
            Files.writeString(
                    destPath, content,
                    StandardCharsets.UTF_8);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(
                    "Failed to render %s: %s"
                            .formatted(templateName,
                                    e.getMessage()));
        }
    }
}
