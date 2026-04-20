package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that rule 21 (CI-Watch) is copied from the
 * classpath resources into the generated {@code .claude/rules/}
 * output. Story-0045-0002, TASK-0045-0002-004.
 *
 * <p>This test uses the real classpath under
 * {@code java/src/main/resources} so that a regression in
 * copying the rule file is surfaced immediately — removing
 * {@code 21-ci-watch.md} from source must fail this test
 * with a clear message.</p>
 */
@DisplayName("RulesAssembler — rule 21 CI-Watch")
class RulesAssemblerCiWatchTest {

    @Test
    @DisplayName("21-ci-watch.md is copied to output"
            + " rules directory")
    void listRules_includesCiWatch(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        RulesAssembler assembler = new RulesAssembler();
        ProjectConfig config = TestConfigBuilder.minimal();

        assembler.assemble(
                config, new TemplateEngine(), outputDir);

        Path ruleFile = outputDir.resolve(
                "rules/21-ci-watch.md");
        assertThat(ruleFile)
                .as("21-ci-watch.md must be copied from "
                        + "source. If this fails, verify "
                        + "that java/src/main/resources/"
                        + "targets/claude/rules/"
                        + "21-ci-watch.md exists.")
                .exists();
    }

    @Test
    @DisplayName("21-ci-watch.md contains all mandatory"
            + " sections")
    void listRules_ciWatch_hasMandatorySections(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        RulesAssembler assembler = new RulesAssembler();
        ProjectConfig config = TestConfigBuilder.minimal();

        assembler.assemble(
                config, new TemplateEngine(), outputDir);

        String content = Files.readString(
                outputDir.resolve(
                        "rules/21-ci-watch.md"),
                StandardCharsets.UTF_8);

        assertThat(content)
                .contains("## Rule")
                .contains("## Fallback Matrix")
                .contains("## Rationale")
                .contains("## Enforcement")
                .contains("## Forbidden");
    }

    @Test
    @DisplayName("21-ci-watch.md contains fallback matrix"
            + " and canonical identifiers")
    void listRules_ciWatch_containsMandatoryContent(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        RulesAssembler assembler = new RulesAssembler();
        ProjectConfig config = TestConfigBuilder.minimal();

        assembler.assemble(
                config, new TemplateEngine(), outputDir);

        String content = Files.readString(
                outputDir.resolve(
                        "rules/21-ci-watch.md"),
                StandardCharsets.UTF_8);

        assertThat(content)
                .contains("V1 no-op")
                .contains("V2 active")
                .contains("V2 skipped")
                .contains("--no-ci-watch")
                .contains("x-pr-watch-ci")
                .contains("x-pr-create")
                .contains("RULE-045-01")
                .contains("audit-rule-20.sh");
    }
}
