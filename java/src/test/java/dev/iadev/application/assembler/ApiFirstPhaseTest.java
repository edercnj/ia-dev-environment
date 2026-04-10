package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for API-First Phase (Phase 0.5) in the lifecycle
 * and x-test-contract-lint conditional skill generation.
 *
 * <p>Covers story-0017-0007 acceptance criteria:
 * <ul>
 *   <li>GK-1: CLI-only sets
 *       has_contract_interfaces=False</li>
 *   <li>GK-2: REST sets
 *       has_contract_interfaces=True</li>
 *   <li>GK-3: Event sets
 *       has_contract_interfaces=True</li>
 *   <li>GK-4: Lifecycle template contains Phase 0.5
 *       conditional with CONTRACT PENDING APPROVAL</li>
 *   <li>GK-5: x-test-contract-lint generated conditionally</li>
 * </ul>
 */
@DisplayName("API-First Phase (story-0017-0007)")
class ApiFirstPhaseTest {

    @Nested
    @DisplayName("ContextBuilder — has_contract_interfaces")
    class ContractInterfacesContext {

        @Test
        @DisplayName("REST interface sets"
                + " has_contract_interfaces to True")
        void buildContext_restInterface_trueValue() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_contract_interfaces"))
                    .isEqualTo("True");
        }

        @Test
        @DisplayName("gRPC interface sets"
                + " has_contract_interfaces to True")
        void buildContext_grpcInterface_trueValue() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("grpc")
                            .build();

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_contract_interfaces"))
                    .isEqualTo("True");
        }

        @Test
        @DisplayName("event-consumer interface sets"
                + " has_contract_interfaces to True")
        void buildContext_eventConsumer_trueValue() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("event-consumer")
                            .build();

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_contract_interfaces"))
                    .isEqualTo("True");
        }

        @Test
        @DisplayName("event-producer interface sets"
                + " has_contract_interfaces to True")
        void buildContext_eventProducer_trueValue() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("event-producer")
                            .build();

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_contract_interfaces"))
                    .isEqualTo("True");
        }

        @Test
        @DisplayName("websocket interface sets"
                + " has_contract_interfaces to True")
        void buildContext_websocket_trueValue() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("websocket")
                            .build();

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_contract_interfaces"))
                    .isEqualTo("True");
        }

        @Test
        @DisplayName("CLI-only interface sets"
                + " has_contract_interfaces to False")
        void buildContext_cliOnly_falseValue() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_contract_interfaces"))
                    .isEqualTo("False");
        }

        @Test
        @DisplayName("empty interfaces sets"
                + " has_contract_interfaces to False")
        void buildContext_noInterfaces_falseValue() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .build();

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_contract_interfaces"))
                    .isEqualTo("False");
        }
    }

    @Nested
    @DisplayName("SkillsSelection — x-test-contract-lint")
    class ContractLintSelection {

        @Test
        @DisplayName("REST interface includes"
                + " x-test-contract-lint")
        void select_rest_includesContractLint() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectInterfaceSkills(config);

            assertThat(skills)
                    .contains("x-test-contract-lint");
        }

        @Test
        @DisplayName("gRPC interface includes"
                + " x-test-contract-lint")
        void select_grpc_includesContractLint() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("grpc")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectInterfaceSkills(config);

            assertThat(skills)
                    .contains("x-test-contract-lint");
        }

        @Test
        @DisplayName("event-consumer includes"
                + " x-test-contract-lint")
        void select_eventConsumer_includesContractLint() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("event-consumer")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectInterfaceSkills(config);

            assertThat(skills)
                    .contains("x-test-contract-lint");
        }

        @Test
        @DisplayName("event-producer includes"
                + " x-test-contract-lint")
        void select_eventProducer_includesContractLint() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("event-producer")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectInterfaceSkills(config);

            assertThat(skills)
                    .contains("x-test-contract-lint");
        }

        @Test
        @DisplayName("websocket includes"
                + " x-test-contract-lint")
        void select_websocket_includesContractLint() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("websocket")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectInterfaceSkills(config);

            assertThat(skills)
                    .contains("x-test-contract-lint");
        }

        @Test
        @DisplayName("CLI-only excludes x-test-contract-lint")
        void select_cliOnly_excludesContractLint() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectInterfaceSkills(config);

            assertThat(skills)
                    .doesNotContain("x-test-contract-lint");
        }
    }

    @Nested
    @DisplayName("Lifecycle template — Phase 0.5 content")
    class LifecyclePhaseContent {

        @Test
        @DisplayName("lifecycle template contains Phase 0.5"
                + " conditional block")
        void assemble_lifecycle_containsPhase05Conditional(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path lifecycle = outputDir.resolve(
                    "skills/x-dev-story-implement/SKILL.md");
            String content = Files.readString(lifecycle);
            assertThat(content)
                    .contains("Phase 0.5");
            assertThat(content)
                    .contains("has_contract_interfaces");
        }

        @Test
        @DisplayName("lifecycle template contains"
                + " CONTRACT PENDING APPROVAL message")
        void assemble_lifecycle_containsPendingApproval(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path lifecycle = outputDir.resolve(
                    "skills/x-dev-story-implement/SKILL.md");
            String content = Files.readString(lifecycle);
            assertThat(content)
                    .contains("CONTRACT PENDING APPROVAL");
        }

        @Test
        @DisplayName("lifecycle template contains"
                + " OpenAPI 3.1 reference in Phase 0.5")
        void assemble_lifecycle_containsOpenApiRef(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path lifecycle = outputDir.resolve(
                    "skills/x-dev-story-implement/SKILL.md");
            String content = Files.readString(lifecycle);
            assertThat(content)
                    .contains("OpenAPI 3.1");
        }

        @Test
        @DisplayName("lifecycle template contains"
                + " AsyncAPI 2.6 reference in Phase 0.5")
        void assemble_lifecycle_containsAsyncApiRef(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path lifecycle = outputDir.resolve(
                    "skills/x-dev-story-implement/SKILL.md");
            String content = Files.readString(lifecycle);
            assertThat(content)
                    .contains("AsyncAPI 2.6");
        }

        @Test
        @DisplayName("lifecycle template contains"
                + " interface-to-format mapping table")
        void assemble_lifecycle_containsFormatTable(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path lifecycle = outputDir.resolve(
                    "skills/x-dev-story-implement/SKILL.md");
            String content = Files.readString(lifecycle);
            assertThat(content)
                    .contains("Protobuf 3");
        }

        @Test
        @DisplayName("lifecycle template references"
                + " x-test-contract-lint skill")
        void assemble_lifecycle_referencesContractLint(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path lifecycle = outputDir.resolve(
                    "skills/x-dev-story-implement/SKILL.md");
            String content = Files.readString(lifecycle);
            assertThat(content)
                    .contains("x-test-contract-lint");
        }
    }

    @Nested
    @DisplayName("x-test-contract-lint skill generation")
    class ContractLintSkillGeneration {

        @Test
        @DisplayName("REST config generates"
                + " x-test-contract-lint skill")
        void assemble_restConfig_generatesContractLint(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/x-test-contract-lint/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("CLI-only config does NOT generate"
                + " x-test-contract-lint skill")
        void assemble_cliOnly_excludesContractLint(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/x-test-contract-lint"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("x-test-contract-lint skill contains"
                + " contract validation content")
        void assemble_restConfig_contractLintHasContent(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path skillMd = outputDir.resolve(
                    "skills/x-test-contract-lint/SKILL.md");
            String content = Files.readString(skillMd);
            assertThat(content)
                    .contains("x-test-contract-lint");
            assertThat(content)
                    .contains("OpenAPI 3.1");
            assertThat(content)
                    .contains("AsyncAPI 2.6");
            assertThat(content)
                    .contains("Protobuf 3");
        }
    }
}
