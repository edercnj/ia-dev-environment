package dev.iadev.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions
        .assertThatThrownBy;

/**
 * Tests for CicdContext — immutable CI/CD context record.
 */
@DisplayName("CicdContext")
class CicdContextTest {

    @Nested
    @DisplayName("defensive copy of ctx map (L-007)")
    class DefensiveCopy {

        @Test
        @DisplayName("ctx map is immutable after"
                + " construction")
        void create_whenCalled_ctxIsImmutable() {
            Map<String, Object> mutable =
                    new HashMap<>();
            mutable.put("key", "value");

            ProjectConfig config =
                    TestConfigBuilder.minimal();
            CicdContext ctx = new CicdContext(
                    config,
                    Path.of("/tmp/out"),
                    Path.of("/tmp/res"),
                    new TemplateEngine(),
                    mutable);

            assertThatThrownBy(() ->
                    ctx.ctx().put("new", "value"))
                    .isInstanceOf(
                            UnsupportedOperationException
                                    .class);
        }

        @Test
        @DisplayName("modifying original map does not"
                + " affect context")
        void create_whenCalled_originalMapModificationSafe() {
            Map<String, Object> mutable =
                    new HashMap<>();
            mutable.put("key", "original");

            ProjectConfig config =
                    TestConfigBuilder.minimal();
            CicdContext ctx = new CicdContext(
                    config,
                    Path.of("/tmp/out"),
                    Path.of("/tmp/res"),
                    new TemplateEngine(),
                    mutable);

            mutable.put("key", "modified");

            assertThat(ctx.ctx().get("key"))
                    .isEqualTo("original");
        }
    }

    @Nested
    @DisplayName("accessors")
    class Accessors {

        @Test
        @DisplayName("returns all fields correctly")
        void create_whenCalled_returnsAllFields() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            Path outputDir = Path.of("/tmp/out");
            Path resourcesDir = Path.of("/tmp/res");
            TemplateEngine engine = new TemplateEngine();
            Map<String, Object> map = Map.of("k", "v");

            CicdContext ctx = new CicdContext(
                    config, outputDir, resourcesDir,
                    engine, map);

            assertThat(ctx.config()).isSameAs(config);
            assertThat(ctx.outputDir())
                    .isEqualTo(outputDir);
            assertThat(ctx.resourcesDir())
                    .isEqualTo(resourcesDir);
            assertThat(ctx.engine()).isSameAs(engine);
            assertThat(ctx.ctx()).containsEntry("k", "v");
        }
    }
}
