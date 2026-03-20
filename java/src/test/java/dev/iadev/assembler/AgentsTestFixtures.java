package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;

/**
 * Shared test fixtures for AgentsAssembler tests.
 */
final class AgentsTestFixtures {

    private AgentsTestFixtures() {
    }

    static ProjectConfig buildGoGinConfig() {
        return TestConfigBuilder.builder()
                .projectName("my-go-service")
                .purpose(
                        "Describe your service purpose here")
                .archStyle("microservice")
                .domainDriven(false)
                .eventDriven(true)
                .language("go", "1.22")
                .framework("gin", "")
                .buildTool("go-mod")
                .nativeBuild(false)
                .database("postgresql", "17")
                .cache("redis", "7.4")
                .container("docker")
                .orchestrator("kubernetes")
                .iac("terraform")
                .apiGateway("kong")
                .securityFrameworks("lgpd")
                .smokeTests(true)
                .contractTests(false)
                .performanceTests(true)
                .clearInterfaces()
                .addInterface("rest")
                .addInterface("grpc")
                .addInterface("event-consumer")
                .addInterface("event-producer")
                .build();
    }
}
