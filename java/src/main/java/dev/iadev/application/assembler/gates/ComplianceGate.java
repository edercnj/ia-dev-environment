package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;

import java.util.List;

/**
 * Contributes {@code x-review-compliance} when {@code pci-dss}
 * is declared in the compliance frameworks.
 */
public final class ComplianceGate
        implements SkillGateEvaluator {

    @Override
    public List<String> evaluate(ProjectConfig config) {
        if (config.compliance().contains("pci-dss")) {
            return List.of("x-review-compliance");
        }
        return List.of();
    }
}
