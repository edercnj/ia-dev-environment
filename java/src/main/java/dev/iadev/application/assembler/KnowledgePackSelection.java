package dev.iadev.application.assembler;

import dev.iadev.domain.stack.SkillRegistry;
import dev.iadev.domain.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure knowledge-pack selection functions based on project
 * config feature gates.
 *
 * <p>Extracted from {@link SkillsSelection} to keep both
 * classes under 250 lines per RULE-004. No file I/O.</p>
 *
 * @see SkillsSelection
 * @see SkillRegistry
 */
public final class KnowledgePackSelection {

    private KnowledgePackSelection() {
        // utility class
    }

    /**
     * Determines which knowledge packs to include.
     *
     * <p>Always includes core knowledge packs plus
     * layer-templates. Conditionally includes
     * database-patterns, data-modeling,
     * disaster-recovery, finops,
     * architecture-cqrs, architecture-hexagonal,
     * ddd-strategic, and patterns-outbox.</p>
     *
     * @param config the project configuration
     * @return list of knowledge pack names to include
     */
    public static List<String> selectKnowledgePacks(
            ProjectConfig config) {
        List<String> packs = new ArrayList<>(
                SkillRegistry.CORE_KNOWLEDGE_PACKS);
        packs.add("layer-templates");
        packs.addAll(selectDataPacks(config));
        packs.addAll(selectOutboxPack(config));
        packs.addAll(selectDisasterRecoveryPack(config));
        packs.addAll(selectCloudPacks(config));
        packs.addAll(selectArchitecturePacks(config));
        packs.addAll(selectDddStrategicPack(config));
        packs.addAll(selectPciDssRequirementsPack(config));
        packs.addAll(selectOwaspAsvsReferencePack(config));
        return packs;
    }

    private static List<String> selectArchitecturePacks(
            ProjectConfig config) {
        String style = config.architecture().style();
        if ("cqrs".equals(style)) {
            return List.of("architecture-cqrs");
        }
        if ("hexagonal".equals(style)) {
            return List.of("architecture-hexagonal");
        }
        return List.of();
    }

    private static List<String> selectDataPacks(
            ProjectConfig config) {
        if (!"none".equals(config.data().database().name())
                || !"none".equals(
                        config.data().cache().name())) {
            return List.of(
                    "database-patterns", "data-modeling");
        }
        return List.of();
    }

    private static List<String> selectOutboxPack(
            ProjectConfig config) {
        if (config.architecture().outboxPattern()) {
            return List.of("patterns-outbox");
        }
        return List.of();
    }

    private static List<String> selectDisasterRecoveryPack(
            ProjectConfig config) {
        if (!"none".equals(
                config.infrastructure().container())) {
            return List.of("disaster-recovery");
        }
        return List.of();
    }

    private static List<String> selectCloudPacks(
            ProjectConfig config) {
        String provider =
                config.infrastructure().cloudProvider();
        if (provider != null
                && !provider.isEmpty()
                && !"none".equals(provider)) {
            return List.of("finops");
        }
        return List.of();
    }

    private static List<String> selectDddStrategicPack(
            ProjectConfig config) {
        String style = config.architecture().style();
        if ("hexagonal".equals(style)
                || "ddd".equals(style)
                || config.architecture().dddEnabled()) {
            return List.of("ddd-strategic");
        }
        return List.of();
    }

    private static List<String> selectPciDssRequirementsPack(
            ProjectConfig config) {
        if (config.security().frameworks()
                .contains("pci-dss")) {
            return List.of("pci-dss-requirements");
        }
        return List.of();
    }

    private static List<String> selectOwaspAsvsReferencePack(
            ProjectConfig config) {
        if (config.security().frameworks()
                .contains("owasp-asvs")) {
            return List.of("owasp-asvs");
        }
        return List.of();
    }
}
