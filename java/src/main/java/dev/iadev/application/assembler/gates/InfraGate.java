package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Contributes skills based on infrastructure configuration:
 * observability tool, orchestrator, and API gateway presence.
 */
public final class InfraGate implements SkillGateEvaluator {

    @Override
    public List<String> evaluate(ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        if (!"none".equalsIgnoreCase(
                config.infrastructure().observability()
                        .tool())) {
            skills.add("x-obs-instrument");
        }
        if (!"none".equalsIgnoreCase(
                config.infrastructure().orchestrator())) {
            skills.add("setup-environment");
        }
        if (!"none".equalsIgnoreCase(
                config.infrastructure().apiGateway())) {
            skills.add("x-review-gateway");
        }
        return skills;
    }
}
