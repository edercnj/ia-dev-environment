package dev.iadev.application.assembler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generates Kubernetes manifests (deployment.yaml,
 * service.yaml, configmap.yaml) conditionally based on
 * {@code orchestrator == "kubernetes"}.
 *
 * @see CicdAssembler
 */
final class K8sManifestAssembler {

    private static final String CICD_TEMPLATES =
            "cicd-templates";
    private static final String K8S_CONDITION =
            "kubernetes";
    private static final List<String> K8S_MANIFESTS =
            List.of(
                    "deployment.yaml",
                    "service.yaml",
                    "configmap.yaml");

    /**
     * Generates K8s manifests if orchestrator is kubernetes.
     *
     * @param cicdCtx the CI/CD context
     * @return the generation result
     */
    CicdResult assemble(CicdContext cicdCtx) {
        if (!K8S_CONDITION.equals(
                cicdCtx.config().infrastructure()
                        .orchestrator())) {
            return new CicdResult(
                    List.of(),
                    List.of("K8s manifests skipped:"
                            + " orchestrator is not"
                            + " kubernetes"));
        }
        List<String> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (String manifest : K8S_MANIFESTS) {
            Path dest = cicdCtx.outputDir()
                    .resolve("k8s").resolve(manifest);
            String tpl = "k8s/"
                    + manifest.replace(
                    ".yaml", ".yaml.njk");
            Optional<String> err = renderAndWrite(
                    cicdCtx, tpl, dest);
            if (err.isEmpty()) {
                files.add(dest.toString());
            } else {
                warnings.add(err.orElseThrow());
            }
        }
        return new CicdResult(files, warnings);
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
