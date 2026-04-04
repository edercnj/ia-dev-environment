package dev.iadev.assembler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates smoke test configuration conditionally based
 * on {@code smokeTests == true}.
 *
 * <p>Copies a static smoke-config.md file from resources
 * to the output directory.</p>
 *
 * @see CicdAssembler
 */
final class SmokeTestAssembler {

    private static final String CICD_TEMPLATES =
            "cicd-templates";
    private static final String SMOKE_SOURCE =
            "smoke-tests/smoke-config.md";

    /**
     * Generates smoke test config if smoke tests are
     * enabled and the source file exists.
     *
     * @param cicdCtx the CI/CD context
     * @return the generation result
     */
    CicdResult assemble(CicdContext cicdCtx) {
        if (!cicdCtx.config().testing().smokeTests()) {
            return new CicdResult(
                    List.of(),
                    List.of("Smoke test config skipped:"
                            + " smokeTests is false"));
        }
        Path src = cicdCtx.resourcesDir()
                .resolve(CICD_TEMPLATES)
                .resolve(SMOKE_SOURCE);
        if (!Files.exists(src)) {
            return CicdResult.empty();
        }
        Path dest = cicdCtx.outputDir()
                .resolve("tests")
                .resolve("smoke")
                .resolve("smoke-config.md");
        CopyHelpers.ensureDirectory(dest.getParent());
        CopyHelpers.copyStaticFile(src, dest);
        return new CicdResult(
                List.of(dest.toString()),
                List.of());
    }
}
