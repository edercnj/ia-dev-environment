package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterfaceGate")
class InterfaceGateTest {

    private final InterfaceGate gate = new InterfaceGate();

    @Test
    @DisplayName("REST interface includes x-review-api and"
            + " x-test-contract-lint")
    void evaluate_rest_includesApiAndContract() {
        ProjectConfig config = TestConfigBuilder.builder()
                .clearInterfaces()
                .addInterface("rest")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills)
                .contains("x-review-api")
                .contains("x-test-contract-lint");
    }

    @Test
    @DisplayName("gRPC interface includes x-review-grpc")
    void evaluate_grpc_includesReviewGrpc() {
        ProjectConfig config = TestConfigBuilder.builder()
                .clearInterfaces()
                .addInterface("grpc")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-review-grpc");
    }

    @Test
    @DisplayName("GraphQL interface includes x-review-graphql")
    void evaluate_graphql_includesReviewGraphql() {
        ProjectConfig config = TestConfigBuilder.builder()
                .clearInterfaces()
                .addInterface("graphql")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-review-graphql");
    }

    @Test
    @DisplayName("event-consumer includes x-review-events")
    void evaluate_eventConsumer_includesEventsReview() {
        ProjectConfig config = TestConfigBuilder.builder()
                .clearInterfaces()
                .addInterface("event-consumer")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-review-events");
    }

    @Test
    @DisplayName("no supported interfaces returns empty")
    void evaluate_noInterfaces_returnsEmpty() {
        ProjectConfig config = TestConfigBuilder.builder()
                .clearInterfaces()
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).isEmpty();
    }

    @Test
    @DisplayName("websocket alone includes contract-lint"
            + " but not x-review-api")
    void evaluate_websocket_includesContractLintOnly() {
        ProjectConfig config = TestConfigBuilder.builder()
                .clearInterfaces()
                .addInterface("websocket")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills)
                .contains("x-test-contract-lint")
                .doesNotContain("x-review-api");
    }
}
