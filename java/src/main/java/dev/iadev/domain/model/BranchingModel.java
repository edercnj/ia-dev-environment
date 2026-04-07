package dev.iadev.domain.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Represents the Git branching strategy for a project.
 *
 * <p>Determines how branch references are resolved in
 * generated skills and CI/CD artifacts:
 * <ul>
 *   <li>{@link #GITFLOW} — base branch is {@code develop},
 *       uses release branches</li>
 *   <li>{@link #TRUNK} — base branch is {@code main},
 *       tags directly on main</li>
 * </ul>
 *
 * @see ProjectConfig
 */
public enum BranchingModel {

    /** Git Flow: develop as base, release branches. */
    GITFLOW("gitflow", "develop"),

    /** Trunk-Based: main as base, direct tags. */
    TRUNK("trunk", "main");

    private final String configValue;
    private final String baseBranch;

    BranchingModel(String configValue, String baseBranch) {
        this.configValue = configValue;
        this.baseBranch = baseBranch;
    }

    /**
     * Returns the YAML config value for this model.
     *
     * @return lowercase config value (e.g., "gitflow")
     */
    public String configValue() {
        return configValue;
    }

    /**
     * Returns the base branch name for this model.
     *
     * @return "develop" for GITFLOW, "main" for TRUNK
     */
    public String baseBranch() {
        return baseBranch;
    }

    /**
     * Resolves a branching model from its config value.
     *
     * <p>Matching is case-insensitive.</p>
     *
     * @param value the config value, may be null
     * @return the matching model, or empty if not found
     */
    public static Optional<BranchingModel> fromConfigValue(
            String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.strip()
                .toLowerCase(Locale.ROOT);
        for (BranchingModel model : values()) {
            if (model.configValue.equals(normalized)) {
                return Optional.of(model);
            }
        }
        return Optional.empty();
    }
}
