package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;

import java.util.List;

/**
 * Contributes {@code x-review-security} when at least one
 * security framework is configured.
 */
public final class SecurityGate implements SkillGateEvaluator {

    @Override
    public List<String> evaluate(ProjectConfig config) {
        if (!config.security().frameworks().isEmpty()) {
            return List.of("x-review-security");
        }
        return List.of();
    }
}
