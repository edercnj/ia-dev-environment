package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pure skill selection functions based on project config
 * feature gates.
 *
 * <p>These functions evaluate config conditions and return
 * skill names. No file I/O — consumed by
 * {@link SkillsAssembler} for assembly decisions.</p>
 *
 * <p>Knowledge-pack selection is delegated to
 * {@link KnowledgePackSelection}.</p>
 *
 * @see SkillsAssembler
 * @see KnowledgePackSelection
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
        if (hasAnyInterface(config,
                "rest", "grpc",
                "event-consumer", "event-producer",
                "websocket")) {
            skills.add("x-contract-lint");
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
     * Selects skills based on security scanning flags,
     * pentest, and quality gate provider.
     *
     * @param config the project configuration
     * @return list of conditional scanning skill names
     */
    public static List<String> selectSecurityScanningSkills(
            ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        var scanning = config.security().scanning();
        if (scanning.sast()) {
            skills.add("x-sast-scan");
        }
        if (scanning.dast()) {
            skills.add("x-dast-scan");
        }
        if (scanning.secretScan()) {
            skills.add("x-secret-scan");
        }
        if (scanning.containerScan()) {
            skills.add("x-container-scan");
        }
        if (scanning.infraScan()) {
            skills.add("x-infra-scan");
        }
        var qgProvider =
                config.security().qualityGate().provider();
        if (!"none".equals(qgProvider)) {
            skills.add("x-sonar-gate");
        }
        return skills;
    }

    /**
     * Selects skills based on compliance frameworks.
     *
     * <p>Includes {@code x-review-compliance} when
     * {@code pci-dss} is present in the security
     * compliance frameworks list.</p>
     *
     * @param config the project configuration
     * @return list of conditional compliance skill names
     */
    public static List<String> selectComplianceSkills(
            ProjectConfig config) {
        if (config.compliance().contains("pci-dss")) {
            return List.of("x-review-compliance");
        }
        return List.of();
    }

    /**
     * Selects pentest skills based on security config.
     *
     * <p>Includes {@code x-pentest} when
     * {@code pentest} is enabled in the security
     * configuration.</p>
     *
     * @param config the project configuration
     * @return list of conditional pentest skill names
     */
    public static List<String> selectPentestSkills(
            ProjectConfig config) {
        if (config.security().pentest()) {
            return List.of("x-pentest");
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
        skills.addAll(selectSecurityScanningSkills(config));
        skills.addAll(selectComplianceSkills(config));
        skills.addAll(selectPentestSkills(config));
        return skills;
    }

    /**
     * Delegates to {@link KnowledgePackSelection}.
     *
     * @param config the project configuration
     * @return list of knowledge pack names to include
     */
    public static List<String> selectKnowledgePacks(
            ProjectConfig config) {
        return KnowledgePackSelection
                .selectKnowledgePacks(config);
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
