package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.Platform;
import dev.iadev.exception.PipelineException;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AssemblerFilterStrategy} — per-descriptor
 * selection and execution decoupled from the pipeline
 * orchestrator.
 */
@DisplayName("AssemblerFilterStrategy")
class AssemblerFilterStrategyTest {

    @Test
    @DisplayName("shouldRun returns true for any descriptor"
            + " in default configuration (happy path)")
    void shouldRun_defaultConfig_returnsTrue() {
        AssemblerFilterStrategy strategy =
                new AssemblerFilterStrategy(
                        TestConfigBuilder.minimal(),
                        PipelineOptions.defaults());
        AssemblerDescriptor desc = descriptor(
                "Any", (c, e, p) -> List.of("f.md"));

        assertThat(strategy.shouldRun(desc)).isTrue();
    }

    @Test
    @DisplayName("shouldRun rejects null descriptor")
    void shouldRun_nullDescriptor_throwsNpe() {
        AssemblerFilterStrategy strategy =
                new AssemblerFilterStrategy(
                        TestConfigBuilder.minimal(),
                        PipelineOptions.defaults());

        assertThatNullPointerException()
                .isThrownBy(() -> strategy.shouldRun(null));
    }

    @Test
    @DisplayName("executeAll runs every descriptor in order"
            + " and aggregates files")
    void executeAll_happyPath_aggregatesFilesInOrder(
            @TempDir Path tempDir) {
        List<String> executed = new ArrayList<>();
        AssemblerFilterStrategy strategy =
                new AssemblerFilterStrategy(
                        TestConfigBuilder.minimal(),
                        PipelineOptions.defaults());
        List<AssemblerDescriptor> descriptors = List.of(
                descriptor("A",
                        tracking(executed, "A", "a.md")),
                descriptor("B",
                        tracking(executed, "B", "b.md")),
                descriptor("C",
                        tracking(executed, "C", "c.md")));

        AssemblerResult result = strategy.executeAll(
                descriptors, tempDir, new TemplateEngine());

        assertThat(executed)
                .containsExactly("A", "B", "C");
        assertThat(result.files())
                .containsExactly("a.md", "b.md", "c.md");
    }

    @Test
    @DisplayName("executeAll wraps runtime exceptions in"
            + " PipelineException naming the descriptor")
    void executeAll_runtimeException_wrapsInPipelineException(
            @TempDir Path tempDir) {
        AssemblerFilterStrategy strategy =
                new AssemblerFilterStrategy(
                        TestConfigBuilder.minimal(),
                        PipelineOptions.defaults());
        AssemblerDescriptor fail = descriptor(
                "Broken", (c, e, p) -> {
                    throw new RuntimeException("boom");
                });

        assertThatThrownBy(() -> strategy.executeAll(
                List.of(fail), tempDir,
                new TemplateEngine()))
                .isInstanceOf(PipelineException.class)
                .hasMessageContaining("Broken");
    }

    @Test
    @DisplayName("constructor rejects null config")
    void constructor_nullConfig_throwsNpe() {
        assertThatNullPointerException().isThrownBy(() ->
                new AssemblerFilterStrategy(
                        (ProjectConfig) null,
                        PipelineOptions.defaults()));
    }

    @Test
    @DisplayName("constructor rejects null options")
    void constructor_nullOptions_throwsNpe() {
        assertThatNullPointerException().isThrownBy(() ->
                new AssemblerFilterStrategy(
                        TestConfigBuilder.minimal(),
                        null));
    }

    @Test
    @DisplayName("config() and options() expose the"
            + " injected collaborators")
    void accessors_exposeInjectedCollaborators() {
        ProjectConfig config = TestConfigBuilder.minimal();
        PipelineOptions options =
                PipelineOptions.defaults();

        AssemblerFilterStrategy strategy =
                new AssemblerFilterStrategy(
                        config, options);

        assertThat(strategy.config()).isSameAs(config);
        assertThat(strategy.options()).isSameAs(options);
    }

    private static AssemblerDescriptor descriptor(
            String name, Assembler assembler) {
        return new AssemblerDescriptor(
                name, AssemblerTarget.ROOT,
                Set.of(Platform.SHARED), assembler);
    }

    private static Assembler tracking(
            List<String> tracker, String name,
            String file) {
        return (config, engine, outputDir) -> {
            tracker.add(name);
            return List.of(file);
        };
    }
}
