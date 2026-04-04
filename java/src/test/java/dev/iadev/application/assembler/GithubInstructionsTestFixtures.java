package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;

/**
 * Shared test fixtures for
 * GithubInstructionsAssembler tests.
 */
final class GithubInstructionsTestFixtures {

    private GithubInstructionsTestFixtures() {
    }

    static ProjectConfig buildRustAxumConfig() {
        return TestConfigBuilder.builder()
                .projectName("my-rust-service")
                .purpose("Describe your service"
                        + " purpose here")
                .archStyle("microservice")
                .domainDriven(false)
                .eventDriven(true)
                .language("rust", "2024")
                .framework("axum", "")
                .buildTool("cargo")
                .nativeBuild(false)
                .container("docker")
                .orchestrator("kubernetes")
                .smokeTests(true)
                .contractTests(false)
                .clearInterfaces()
                .addInterface("rest")
                .addInterface("grpc")
                .addInterface("event-consumer")
                .addInterface("event-producer")
                .build();
    }
}
