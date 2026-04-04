package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;

/**
 * Shared test fixtures for SkillsAssembler tests.
 */
final class SkillsTestFixtures {

    private SkillsTestFixtures() {
    }

    static ProjectConfig buildQuarkusConfig() {
        return TestConfigBuilder.builder()
                .projectName("my-quarkus-service")
                .purpose(
                        "Describe your service purpose here")
                .archStyle("microservice")
                .domainDriven(true)
                .eventDriven(true)
                .language("java", "21")
                .framework("quarkus", "3.17")
                .buildTool("maven")
                .nativeBuild(true)
                .contractTests(true)
                .orchestrator("kubernetes")
                .clearInterfaces()
                .addInterface("rest")
                .addInterface("grpc")
                .addInterface("event-consumer")
                .addInterface("event-producer")
                .build();
    }
}
