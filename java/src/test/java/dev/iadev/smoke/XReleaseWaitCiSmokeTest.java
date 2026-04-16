package dev.iadev.smoke;

import dev.iadev.application.assembler.SkillsAssembler;
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

@DisplayName("XReleaseWaitCiSmokeTest")
class XReleaseWaitCiSmokeTest {

    @Test
    @DisplayName("waitCi_greenRedTimeoutNoWait_pathsAreDocumented")
    void waitCi_greenRedTimeoutNoWait_pathsAreDocumented(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        SkillsAssembler assembler = new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(),
                outputDir);

        String skill = Files.readString(
                outputDir.resolve("skills/x-release/SKILL.md"),
                StandardCharsets.UTF_8);
        String schema = Files.readString(
                outputDir.resolve(
                        "skills/x-release/references/"
                                + "state-file-schema.md"),
                StandardCharsets.UTF_8);

        assertThat(skill)
                .contains("Step 7.8 — WAIT-CI")
                .contains("RELEASE_CI_FAILED")
                .contains("RELEASE_CI_TIMEOUT")
                .contains("--no-wait-ci")
                .contains("--ci-timeout <minutes>")
                .contains("ciStatus = \"PASS\"")
                .contains("ciStatus = \"FAIL\"")
                .contains("ciStatus = \"TIMEOUT\"");

        assertThat(schema)
                .contains("noWaitCi")
                .contains("ciCheckedAt")
                .contains("ciStatus")
                .contains("PASS")
                .contains("FAIL")
                .contains("TIMEOUT");
    }
}
