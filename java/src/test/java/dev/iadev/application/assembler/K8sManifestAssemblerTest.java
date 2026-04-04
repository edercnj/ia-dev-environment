package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for K8sManifestAssembler — generates Kubernetes
 * manifests conditionally.
 */
@DisplayName("K8sManifestAssembler")
class K8sManifestAssemblerTest {

    @Nested
    @DisplayName("assemble — orchestrator=kubernetes")
    class OrchestratorK8s {

        @Test
        @DisplayName("generates 3 K8s manifests")
        void assemble_k8s_generatesManifests(
                @TempDir Path tempDir) {
            K8sManifestAssembler assembler =
                    new K8sManifestAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .orchestrator("kubernetes")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            CicdResult result =
                    assembler.assemble(cicdCtx);

            assertThat(result.files()).hasSize(3);
            assertThat(result.files())
                    .anyMatch(f -> f.contains(
                            "deployment.yaml"))
                    .anyMatch(f -> f.contains(
                            "service.yaml"))
                    .anyMatch(f -> f.contains(
                            "configmap.yaml"));
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("K8s files exist on disk")
        void assemble_k8s_filesExist(
                @TempDir Path tempDir) {
            K8sManifestAssembler assembler =
                    new K8sManifestAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .orchestrator("kubernetes")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            assembler.assemble(cicdCtx);

            assertThat(tempDir.resolve(
                    "k8s/deployment.yaml")).exists();
            assertThat(tempDir.resolve(
                    "k8s/service.yaml")).exists();
            assertThat(tempDir.resolve(
                    "k8s/configmap.yaml")).exists();
        }
    }

    @Nested
    @DisplayName("assemble — orchestrator=none")
    class OrchestratorNone {

        @Test
        @DisplayName("skips K8s manifests when"
                + " orchestrator=none")
        void assemble_none_skipsManifests(
                @TempDir Path tempDir) {
            K8sManifestAssembler assembler =
                    new K8sManifestAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .orchestrator("none")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            CicdResult result =
                    assembler.assemble(cicdCtx);

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "K8s manifests skipped"));
        }
    }

    @Nested
    @DisplayName("assemble — style=cqrs + kubernetes")
    class CqrsStatefulSet {

        @Test
        @DisplayName("generates StatefulSet for"
                + " EventStoreDB when style=cqrs")
        void assemble_cqrsK8s_generatesStatefulSet(
                @TempDir Path tempDir) {
            K8sManifestAssembler assembler =
                    new K8sManifestAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .orchestrator("kubernetes")
                    .archStyle("cqrs")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            CicdResult result =
                    assembler.assemble(cicdCtx);

            assertThat(result.files())
                    .anyMatch(f -> f.contains(
                            "eventstore-statefulset"));
        }

        @Test
        @DisplayName("StatefulSet has"
                + " volumeClaimTemplate")
        void assemble_cqrsK8s_hasVolumeClaimTemplate(
                @TempDir Path tempDir)
                throws Exception {
            K8sManifestAssembler assembler =
                    new K8sManifestAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .orchestrator("kubernetes")
                    .archStyle("cqrs")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve("k8s/"
                            + "eventstore-statefulset"
                            + ".yaml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("StatefulSet");
            assertThat(content)
                    .contains("volumeClaimTemplates");
            assertThat(content)
                    .doesNotContain("kind: Deployment");
        }

        @Test
        @DisplayName("no StatefulSet when style is"
                + " not cqrs")
        void assemble_notCqrsK8s_noStatefulSet(
                @TempDir Path tempDir) {
            K8sManifestAssembler assembler =
                    new K8sManifestAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .orchestrator("kubernetes")
                    .archStyle("microservice")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            CicdResult result =
                    assembler.assemble(cicdCtx);

            assertThat(result.files())
                    .noneMatch(f -> f.contains(
                            "eventstore-statefulset"));
        }
    }

    private static CicdContext buildContext(
            ProjectConfig config, Path outputDir) {
        Map<String, Object> stackCtx =
                CicdAssembler.buildStackContext(config);
        Map<String, Object> fullCtx =
                new LinkedHashMap<>(
                        ContextBuilder.buildContext(
                                config));
        fullCtx.putAll(stackCtx);
        return new CicdContext(
                config, outputDir, resolveResources(),
                new TemplateEngine(), fullCtx);
    }

    private static Path resolveResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot("cicd-templates");
    }
}
