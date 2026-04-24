package dev.iadev.testutil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared helper for tests that validate content of a slim
 * ADR-0012 skill together with its {@code references/full-protocol.md}
 * sibling.
 *
 * <p>Introduced by story-0054-0004 (PR #629 review feedback — Copilot
 * comment 3135088538) to replace the ~20 copies of the same
 * "read SKILL.md; append full-protocol.md if present" pattern that
 * were being inlined into individual {@code generateClaudeContent}
 * helpers across Release*Test, SecurityPipelineSkillTest,
 * FixEpicPrCommentsDevelopTest, and LazyKpLoadingTest.</p>
 *
 * <p>Future ADR-0012 migrations are now a one-line change per test:
 * replace the bespoke helper with
 * {@link #readSkillWithReferences(Path, String)}.</p>
 *
 * @see dev.iadev.smoke.Epic0054CompressionSmokeTest
 */
public final class SkillContentReader {

    private SkillContentReader() {
        // utility
    }

    /**
     * Reads {@code skills/<leaf>/SKILL.md} from the supplied
     * assembler output directory and, when a sibling
     * {@code skills/<leaf>/references/full-protocol.md} exists,
     * appends its content separated by a single newline.
     *
     * @param outputDir the assembler output directory (root that
     *                  contains {@code skills/<leaf>/...})
     * @param skillLeaf the skill leaf directory name (e.g.,
     *                  {@code "x-release"})
     * @return concatenated content; never {@code null}
     * @throws IOException when the SKILL.md cannot be read
     */
    public static String readSkillWithReferences(
            Path outputDir, String skillLeaf) throws IOException {
        Path skillMd = outputDir.resolve(
                "skills/" + skillLeaf + "/SKILL.md");
        Path fullProtocol = outputDir.resolve(
                "skills/" + skillLeaf
                        + "/references/full-protocol.md");

        String content = Files.readString(
                skillMd, StandardCharsets.UTF_8);
        if (Files.exists(fullProtocol)) {
            content += "\n"
                    + Files.readString(
                            fullProtocol,
                            StandardCharsets.UTF_8);
        }
        return content;
    }
}
