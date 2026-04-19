package dev.iadev.application.assembler.gates;

import dev.iadev.application.assembler.ConditionEvaluator;
import dev.iadev.domain.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Contributes review / contract-lint skills based on the
 * configured inbound interface types (REST, gRPC, GraphQL,
 * events, WebSocket).
 */
public final class InterfaceGate implements SkillGateEvaluator {

    @Override
    public List<String> evaluate(ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        if (ConditionEvaluator.hasInterface(config, "rest")) {
            skills.add("x-review-api");
        }
        if (ConditionEvaluator.hasInterface(config, "grpc")) {
            skills.add("x-review-grpc");
        }
        if (ConditionEvaluator.hasInterface(
                config, "graphql")) {
            skills.add("x-review-graphql");
        }
        if (ConditionEvaluator.hasAnyInterface(config,
                "event-consumer", "event-producer")) {
            skills.add("x-review-events");
        }
        if (ConditionEvaluator.hasAnyInterface(config,
                "rest", "grpc",
                "event-consumer", "event-producer",
                "websocket")) {
            skills.add("x-test-contract-lint");
        }
        return skills;
    }
}
