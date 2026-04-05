package dev.iadev.application.assembler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared test helpers for RulesAssembler coverage tests.
 */
final class RulesAssemblerCoverageHelper {

    private RulesAssemblerCoverageHelper() {
    }

    static Path setupMinimalRes(Path tempDir)
            throws IOException {
        Path resourceDir = tempDir.resolve("res");
        Path coreRules =
                resourceDir.resolve("core-rules");
        Files.createDirectories(coreRules);
        Path templates =
                resourceDir.resolve("templates");
        Files.createDirectories(templates);
        Files.writeString(
                templates.resolve("domain-template.md"),
                "Domain {DOMAIN_NAME}\n",
                StandardCharsets.UTF_8);
        return resourceDir;
    }
}
