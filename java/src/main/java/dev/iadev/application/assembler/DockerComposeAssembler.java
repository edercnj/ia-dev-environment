package dev.iadev.application.assembler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Generates docker-compose.yml conditionally based on
 * {@code container == "docker"}.
 *
 * @see CicdAssembler
 */
final class DockerComposeAssembler {

    private static final String CICD_TEMPLATES =
            "shared/cicd-templates";
    private static final String COMPOSE_TEMPLATE =
            "docker-compose/docker-compose.yml.njk";
    private static final String DOCKER_CONDITION =
            "docker";

    /**
     * Generates docker-compose.yml if container is docker.
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
                    List.of("Docker Compose skipped:"
                            + " container is not docker"));
        }
        Path dest = cicdCtx.outputDir()
                .resolve("docker-compose.yml");
        Optional<String> err = renderAndWrite(
                cicdCtx, COMPOSE_TEMPLATE, dest);
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
