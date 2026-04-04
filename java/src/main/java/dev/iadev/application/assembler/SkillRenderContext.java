package dev.iadev.application.assembler;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable parameter object for skill rendering operations.
 *
 * <p>Groups the parameters needed by
 * {@link GithubSkillsAssembler#renderSkill},
 * {@link GithubSkillsAssembler#copyReferences}, and the
 * internal {@code generateGroup} method, reducing parameter
 * count from 5-6 to at most 4.</p>
 *
 * @param srcDir   the source template directory
 * @param outputDir the output base directory
 * @param subDir   optional subdirectory for nested groups,
 *                 or null
 * @param context  the template placeholder context map
 */
public record SkillRenderContext(
        Path srcDir,
        Path outputDir,
        String subDir,
        Map<String, Object> context) {

    /**
     * Validates that required fields are not null.
     */
    public SkillRenderContext {
        Objects.requireNonNull(srcDir,
                "srcDir must not be null");
        Objects.requireNonNull(outputDir,
                "outputDir must not be null");
        Objects.requireNonNull(context,
                "context must not be null");
        context = Map.copyOf(context);
    }
}
