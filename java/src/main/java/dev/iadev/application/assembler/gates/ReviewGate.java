package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Contributes review skills based on database, observability,
 * container, and architecture style configuration.
 *
 * <p>Includes {@code x-review-data-modeling} only when a
 * database is configured AND the architecture style is
 * DDD-compatible (hexagonal, ddd, cqrs, or clean).</p>
 */
public final class ReviewGate implements SkillGateEvaluator {

    private static final Set<String> HEXAGONAL_DDD_STYLES =
            Set.of("hexagonal", "ddd", "cqrs", "clean");

    @Override
    public List<String> evaluate(ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        if (!"none".equalsIgnoreCase(
                config.databaseName())) {
            skills.add("x-review-db");
        }
        if (!"none".equalsIgnoreCase(
                config.observabilityTool())) {
            skills.add("x-review-obs");
        }
        if (!"none".equalsIgnoreCase(
                config.infrastructure().container())) {
            skills.add("x-review-devops");
        }
        if (!"none".equalsIgnoreCase(config.databaseName())
                && isHexagonalOrDdd(config)) {
            skills.add("x-review-data-modeling");
        }
        return skills;
    }

    /**
     * Checks if the architecture style supports DDD tactical
     * patterns (hexagonal, ddd, cqrs, clean).
     *
     * @param config the project configuration
     * @return true if architecture style is DDD-compatible
     */
    public static boolean isHexagonalOrDdd(
            ProjectConfig config) {
        String style = config.architecture().style()
                .toLowerCase(Locale.ROOT);
        return HEXAGONAL_DDD_STYLES.contains(style);
    }
}
