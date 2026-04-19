package dev.iadev.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * Policy and meta configuration of a {@link ProjectConfig}.
 *
 * <p>Bundles the four policy / meta fields — compliance type,
 * target platforms, branching model, and telemetry flag — that
 * govern how the generated output is assembled but do not
 * describe the technology stack itself.</p>
 *
 * <p>Extracted by EPIC-0044 (audit finding M-003) to keep the
 * root {@link ProjectConfig} aggregate within the 4-parameter
 * guideline of Rule 03.</p>
 *
 * @param compliance the compliance type (optional,
 *     default "none"); accepted values are documented on
 *     {@link ProjectConfig}
 * @param platforms the target platforms from YAML (optional,
 *     empty = all, immutable)
 * @param branchingModel the branching strategy (optional,
 *     default {@link BranchingModel#GITFLOW})
 * @param telemetryEnabled whether telemetry hooks are injected
 *     (optional, default {@code true}); maps to YAML
 *     {@code telemetry.enabled} (story-0040-0004)
 */
public record Governance(
        String compliance,
        Set<Platform> platforms,
        BranchingModel branchingModel,
        boolean telemetryEnabled) {

    /**
     * Compact constructor enforcing immutability of the
     * {@code platforms} set and applying defaults for the
     * {@code branchingModel}.
     */
    public Governance {
        platforms = platforms == null
                ? Set.of()
                : Set.copyOf(platforms);
        branchingModel = branchingModel == null
                ? BranchingModel.GITFLOW
                : branchingModel;
    }

    /**
     * Creates a Governance record from a YAML-parsed root map.
     *
     * <p>Delegates compliance, platforms, branching-model, and
     * telemetry parsing to the helpers owned by
     * {@link ProjectConfig} so the validation rules remain in a
     * single place.</p>
     *
     * @param root the root map from YAML deserialization
     * @return a new Governance instance populated with defaults
     *     for any missing field
     * @throws ConfigValidationException if the compliance or
     *     branching-model value is invalid
     */
    public static Governance fromMap(Map<String, Object> root) {
        return new Governance(
                ProjectConfig.parseCompliance(root),
                ProjectConfig.parsePlatforms(root),
                ProjectConfig.parseBranchingModel(root),
                ProjectConfig.parseTelemetryEnabled(root));
    }
}
