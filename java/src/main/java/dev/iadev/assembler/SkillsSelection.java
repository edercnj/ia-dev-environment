package dev.iadev.assembler;

import dev.iadev.domain.stack.SkillRegistry;
import dev.iadev.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pure skill selection functions based on project config
 * feature gates.
 *
 * <p>These functions evaluate config conditions and return
 * skill/pack names. No file I/O — consumed by
 * {@link SkillsAssembler} for assembly decisions.</p>
 *
 * <p>Selection categories:
 * <ul>
 *   <li>Interface skills — based on REST, gRPC, GraphQL,
 *       event types</li>
 *   <li>Infrastructure skills — based on observability,
 *       orchestrator, API gateway</li>
 *   <li>Testing skills — based on smoke, performance,
 *       contract testing config</li>
 *   <li>Security skills — based on security frameworks</li>
 *   <li>Knowledge packs — core packs plus data-conditional
 *       packs</li>
 * </ul>
 *
 * @see SkillsAssembler
 * @see SkillRegistry
 */
public final class SkillsSelection {

    private SkillsSelection() {
        // utility class
    }

    /**
     * Selects skills based on interface types.
     *
     * @param config the project configuration
     * @return list of conditional interface skill names
     */
    public static List<String> selectInterfaceSkills(
            ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        if (hasInterface(config, "rest")) {
            skills.add("x-review-api");
        }
        if (hasInterface(config, "grpc")) {
            skills.add("x-review-grpc");
        }
        if (hasInterface(config, "graphql")) {
            skills.add("x-review-graphql");
        }
        if (hasAnyInterface(config,
                "event-consumer", "event-producer")) {
            skills.add("x-review-events");
        }
        return skills;
    }

    /**
     * Selects skills based on infrastructure config.
     *
     * @param config the project configuration
     * @return list of conditional infrastructure skill names
     */
    public static List<String> selectInfraSkills(
            ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        if (!"none".equals(
                config.infrastructure().observability()
                        .tool())) {
            skills.add("instrument-otel");
        }
        if (!"none".equals(
                config.infrastructure().orchestrator())) {
            skills.add("setup-environment");
        }
        if (!"none".equals(
                config.infrastructure().apiGateway())) {
            skills.add("x-review-gateway");
        }
        return skills;
    }

    /**
     * Selects skills based on testing config.
     *
     * @param config the project configuration
     * @return list of conditional testing skill names
     */
    public static List<String> selectTestingSkills(
            ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        if (config.testing().smokeTests()
                && hasInterface(config, "rest")) {
            skills.add("run-smoke-api");
        }
        if (config.testing().smokeTests()
                && hasInterface(config, "tcp-custom")) {
            skills.add("run-smoke-socket");
        }
        skills.add("run-e2e");
        if (config.testing().performanceTests()) {
            skills.add("run-perf-test");
        }
        if (config.testing().contractTests()) {
            skills.add("run-contract-tests");
        }
        return skills;
    }

    /**
     * Selects skills based on security config.
     *
     * @param config the project configuration
     * @return list of conditional security skill names
     */
    public static List<String> selectSecuritySkills(
            ProjectConfig config) {
        if (!config.security().frameworks().isEmpty()) {
            return List.of("x-review-security");
        }
        return List.of();
    }

    /**
     * Evaluates all feature gates and returns the aggregated
     * list of conditional skill names.
     *
     * @param config the project configuration
     * @return aggregated list of all conditional skill names
     */
    public static List<String> selectConditionalSkills(
            ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        skills.addAll(selectInterfaceSkills(config));
        skills.addAll(selectInfraSkills(config));
        skills.addAll(selectTestingSkills(config));
        skills.addAll(selectSecuritySkills(config));
        return skills;
    }

    /**
     * Determines which knowledge packs to include.
     *
     * <p>Always includes core knowledge packs plus
     * layer-templates. Conditionally includes
     * database-patterns if database or cache is not
     * "none".</p>
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
        packs.addAll(
                selectDisasterRecoveryPack(config));
        return packs;
    }

    private static List<String> selectDataPacks(
            ProjectConfig config) {
        if (!"none".equals(config.data().database().name())
                || !"none".equals(
                        config.data().cache().name())) {
            return List.of("database-patterns");
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

    private static boolean hasInterface(
            ProjectConfig config, String type) {
        return config.interfaces().stream()
                .anyMatch(i -> type.equals(i.type()));
    }

    private static boolean hasAnyInterface(
            ProjectConfig config, String... types) {
        Set<String> typeSet = Set.of(types);
        return config.interfaces().stream()
                .anyMatch(i -> typeSet.contains(i.type()));
    }
}
