package dev.iadev.assembler;

import dev.iadev.domain.model.ProjectConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Infrastructure and cloud conditional copy functions
 * for rules assembly.
 *
 * <p>Extracted from {@link RulesConditionals} to keep
 * both classes under 250 lines per RULE-004.</p>
 *
 * @see RulesConditionals
 * @see RulesAssembler
 */
public final class RulesInfraConditionals {

    private static final String NONE_VALUE = "none";

    private RulesInfraConditionals() {
        // utility class
    }

    /**
     * Copies cloud provider files when cloud is configured.
     *
     * @param config      the project configuration
     * @param resourceDir the resources root directory
     * @param skillsDir   the skills output directory
     * @return list of generated file paths
     */
    public static List<String> assembleCloudKnowledge(
            ProjectConfig config,
            Path resourceDir,
            Path skillsDir) {
        String provider =
                config.infrastructure().cloudProvider();
        if (NONE_VALUE.equals(provider)
                || provider == null
                || provider.isEmpty()) {
            return List.of();
        }
        Path cloudDir =
                resourceDir.resolve("cloud-providers");
        Path kpDir =
                skillsDir.resolve("knowledge-packs");
        CopyHelpers.ensureDirectory(kpDir);
        Path src = cloudDir.resolve(provider + ".md");
        if (Files.exists(src)
                && Files.isRegularFile(src)) {
            Path dest = kpDir.resolve(
                    "cloud-" + provider + ".md");
            return List.of(
                    CopyHelpers.copyStaticFile(src, dest));
        }
        return List.of();
    }

    /**
     * Copies infrastructure knowledge packs when infra
     * features are configured.
     *
     * @param config      the project configuration
     * @param resourceDir the resources root directory
     * @param skillsDir   the skills output directory
     * @return list of generated file paths
     */
    public static List<String> assembleInfraKnowledge(
            ProjectConfig config,
            Path resourceDir,
            Path skillsDir) {
        Path infraDir = resourceDir.resolve("infrastructure");
        Path kpDir = skillsDir.resolve("knowledge-packs");
        CopyHelpers.ensureDirectory(kpDir);
        List<String> generated = new ArrayList<>();
        generated.addAll(
                copyK8sFiles(config, infraDir, kpDir));
        generated.addAll(
                copyContainerFiles(config, infraDir, kpDir));
        generated.addAll(
                copyIacFiles(config, infraDir, kpDir));
        return generated;
    }

    static List<String> copyK8sFiles(
            ProjectConfig config,
            Path infraDir, Path kpDir) {
        if (!"kubernetes".equals(
                config.infrastructure().orchestrator())) {
            return List.of();
        }
        Path src = infraDir.resolve(
                "kubernetes/deployment-patterns.md");
        if (Files.exists(src)
                && Files.isRegularFile(src)) {
            Path dest = kpDir.resolve(
                    "k8s-deployment.md");
            return List.of(
                    CopyHelpers.copyStaticFile(src, dest));
        }
        return List.of();
    }

    static List<String> copyContainerFiles(
            ProjectConfig config,
            Path infraDir, Path kpDir) {
        if (NONE_VALUE.equals(
                config.infrastructure().container())) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        String[][] filePairs = {
                {"dockerfile-patterns.md",
                        "dockerfile.md"},
                {"registry-patterns.md",
                        "registry.md"}
        };
        for (String[] pair : filePairs) {
            Path src = infraDir.resolve(
                    "containers/" + pair[0]);
            if (Files.exists(src)
                    && Files.isRegularFile(src)) {
                Path dest = kpDir.resolve(pair[1]);
                generated.add(
                        CopyHelpers.copyStaticFile(
                                src, dest));
            }
        }
        return generated;
    }

    static List<String> copyIacFiles(
            ProjectConfig config,
            Path infraDir, Path kpDir) {
        String iac = config.infrastructure().iac();
        if (NONE_VALUE.equals(iac)
                || iac == null || iac.isEmpty()) {
            return List.of();
        }
        Path src = infraDir.resolve(
                "iac/" + iac + "-patterns.md");
        if (Files.exists(src)
                && Files.isRegularFile(src)) {
            Path dest = kpDir.resolve(
                    "iac-" + iac + ".md");
            return List.of(
                    CopyHelpers.copyStaticFile(src, dest));
        }
        return List.of();
    }
}
