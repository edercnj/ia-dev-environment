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
final class K8sManifestStep {

    private static final String CICD_TEMPLATES =
            "shared/cicd-templates";
    private static final String K8S_CONDITION =
            "kubernetes";
    private static final String CQRS_STYLE = "cqrs";
    private static final String EVENTSTORE_MANIFEST =
            "eventstore-statefulset.yaml";
    private static final List<String> K8S_MANIFESTS =
            List.of(
                    "deployment.yaml",
                    "service.yaml",
                    "configmap.yaml");

    /**
     * Generates K8s manifests if orchestrator is kubernetes.
     * Conditionally adds EventStoreDB StatefulSet when
     * architecture style is cqrs.
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
        renderManifests(cicdCtx, files, warnings);
        renderEventStoreIfCqrs(cicdCtx, files, warnings);
        return new CicdResult(files, warnings);
    }

    private void renderManifests(
            CicdContext cicdCtx,
            List<String> files,
            List<String> warnings) {
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
    }

    private void renderEventStoreIfCqrs(
            CicdContext cicdCtx,
            List<String> files,
            List<String> warnings) {
        if (!CQRS_STYLE.equals(
                cicdCtx.config().architecture().style())) {
            return;
        }
        Path dest = cicdCtx.outputDir()
                .resolve("k8s")
                .resolve(EVENTSTORE_MANIFEST);
        String tpl = "k8s/"
                + EVENTSTORE_MANIFEST
                        .replace(".yaml", ".yaml.njk");
        Optional<String> err = renderAndWrite(
                cicdCtx, tpl, dest);
        if (err.isEmpty()) {
            files.add(dest.toString());
        } else {
            warnings.add(err.orElseThrow());
        }
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
