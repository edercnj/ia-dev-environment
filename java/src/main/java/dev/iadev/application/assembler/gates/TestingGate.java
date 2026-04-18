package dev.iadev.application.assembler.gates;

import dev.iadev.application.assembler.ConditionEvaluator;
import dev.iadev.domain.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Contributes testing-related skills based on smoke,
 * performance, and contract test flags. {@code x-test-e2e}
 * is always included.
 */
public final class TestingGate implements SkillGateEvaluator {

    @Override
    public List<String> evaluate(ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        if (config.testing().smokeTests()
                && ConditionEvaluator.hasInterface(
                        config, "rest")) {
            skills.add("x-test-smoke-api");
        }
        if (config.testing().smokeTests()
                && ConditionEvaluator.hasInterface(
                        config, "tcp-custom")) {
            skills.add("x-test-smoke-socket");
        }
        skills.add("x-test-e2e");
        if (config.testing().performanceTests()) {
            skills.add("x-test-perf");
        }
        if (config.testing().contractTests()) {
            skills.add("x-test-contract");
        }
        return skills;
    }
}
