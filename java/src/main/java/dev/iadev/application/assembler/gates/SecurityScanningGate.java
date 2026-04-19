package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Contributes security-scanning skills based on SAST, DAST,
 * secret-scan, container-scan, infra-scan flags, and the
 * quality-gate provider selection.
 */
public final class SecurityScanningGate
        implements SkillGateEvaluator {

    @Override
    public List<String> evaluate(ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        var scanning = config.security().scanning();
        if (scanning.sast()) {
            skills.add("x-security-sast");
        }
        if (scanning.dast()) {
            skills.add("x-security-dast");
        }
        if (scanning.secretScan()) {
            skills.add("x-security-secrets");
        }
        if (scanning.containerScan()) {
            skills.add("x-security-container");
        }
        if (scanning.infraScan()) {
            skills.add("x-security-infra");
        }
        String qgProvider =
                config.security().qualityGate().provider();
        if (!"none".equalsIgnoreCase(qgProvider)) {
            skills.add("x-security-sonar");
        }
        return skills;
    }
}
