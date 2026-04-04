package dev.iadev.application.assembler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Generates Dockerfile conditionally based on
 * {@code container == "docker"}.
 *
 * <p>Also checks that a stack-specific Dockerfile template
 * exists before rendering.</p>
 *
 * @see CicdAssembler
 */
final class DockerfileAssembler {

    private static final String CICD_TEMPLATES =
            "cicd-templates";
    private static final String DOCKER_CONDITION =
            "docker";

    /**
     * Generates a Dockerfile if container is docker and
     * a matching template exists.
     *
     * @param cicdCtx the CI/CD context
     * @return the generation result
     */
    CicdResult assemble(CicdContext cicdCtx) {
        if (!DOCKER_CONDITION.equals(
                cicdCtx.config().infrastructure()
                        .container())) {
            return new CicdResult(
                    List.of(),
                    List.of("Dockerfile skipped:"
                            + " container is not docker"));
        }
        String stackKey = cicdCtx.config().language().name()
                + "-"
                + cicdCtx.config().framework().buildTool();
        String tpl = "dockerfile/Dockerfile."
                + stackKey + ".njk";
        Path srcPath = cicdCtx.resourcesDir()
                .resolve(CICD_TEMPLATES).resolve(tpl);
        if (!Files.exists(srcPath)) {
            return new CicdResult(
                    List.of(),
                    List.of("Dockerfile template not found"
                            + " for stack: %s"
                                    .formatted(stackKey)));
        }
        Path dest = cicdCtx.outputDir()
                .resolve("Dockerfile");
        Optional<String> err = renderAndWrite(
                cicdCtx, tpl, dest);
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
