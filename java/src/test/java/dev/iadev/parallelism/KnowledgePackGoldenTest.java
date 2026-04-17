package dev.iadev.parallelism;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Story-0041-0001 validation: the {@code parallelism-heuristics} knowledge
 * pack exists at the source-of-truth path, matches its checked-in golden
 * copy byte-for-byte, and declares every section required by the story
 * contract (Footprint Format, Conflict Categories, Hotspot Catalog,
 * Degradation Policy, Examples).
 *
 * <p>The KP is not executable Java — we cannot "invoke" it from a unit
 * test. Instead we validate its content: a single source of truth file
 * that downstream planning skills (x-task-plan, x-story-plan, x-epic-map,
 * x-parallel-eval, x-dev-*-implement) read and interpret at runtime. The
 * golden fixture under {@code src/test/resources/golden/parallelism-heuristics/}
 * is the authoritative reference — drift between the source-of-truth and
 * the golden fails this test and forces explicit review via
 * {@link dev.iadev.golden.GoldenFileRegenerator} (or a manual cp).</p>
 */
@DisplayName("parallelism-heuristics knowledge pack golden")
class KnowledgePackGoldenTest {

    private static final Path KP_SOURCE = Path.of(
            "src", "main", "resources", "targets", "claude", "skills",
            "knowledge-packs", "parallelism-heuristics", "SKILL.md");

    private static final Path KP_GOLDEN = Path.of(
            "src", "test", "resources", "golden",
            "parallelism-heuristics", "SKILL.md");

    @Nested
    @DisplayName("source-of-truth presence")
    class Presence {

        @Test
        void kpSkillMd_existsAtSourceOfTruthPath() {
            assertThat(KP_SOURCE)
                    .as("parallelism-heuristics KP must live at %s", KP_SOURCE)
                    .exists()
                    .isRegularFile();
        }

        @Test
        void kpSkillMd_isNotEmpty() throws IOException {
            long size = Files.size(KP_SOURCE);
            assertThat(size)
                    .as("KP SKILL.md must not be empty")
                    .isGreaterThan(500L);
        }
    }

    @Nested
    @DisplayName("byte-for-byte parity with golden")
    class GoldenParity {

        @Test
        void kpSkillMd_matchesGoldenByteForByte() throws IOException {
            String source = Files.readString(KP_SOURCE, StandardCharsets.UTF_8);
            String golden = Files.readString(KP_GOLDEN, StandardCharsets.UTF_8);
            assertThat(source)
                    .as("source-of-truth KP must match the checked-in "
                            + "golden at %s — if the KP was intentionally "
                            + "updated, copy it to the golden path and "
                            + "re-run this test", KP_GOLDEN)
                    .isEqualTo(golden);
        }
    }

    @Nested
    @DisplayName("required sections")
    class Sections {

        @Test
        void kpSkillMd_declaresAllFiveRequiredSections() throws IOException {
            String md = Files.readString(KP_SOURCE, StandardCharsets.UTF_8);
            assertThat(md)
                    .contains("## Footprint Format")
                    .contains("## Conflict Categories")
                    .contains("## Hotspot Catalog")
                    .contains("## Degradation Policy")
                    .contains("## Examples");
        }

        @Test
        void kpSkillMd_frontmatterMarksItAsKnowledgePack() throws IOException {
            String md = Files.readString(KP_SOURCE, StandardCharsets.UTF_8);
            assertThat(md)
                    .as("KP must declare user-invocable: false so it is "
                            + "not listed as a slash command")
                    .contains("user-invocable: false")
                    .contains("name: parallelism-heuristics");
        }

        @Test
        void kpSkillMd_enumeratesThreeConflictCategories() throws IOException {
            String md = Files.readString(KP_SOURCE, StandardCharsets.UTF_8);
            assertThat(md)
                    .contains("Hard")
                    .contains("Regen")
                    .contains("Soft");
        }

        @Test
        void kpSkillMd_includesCriticalHotspots() throws IOException {
            String md = Files.readString(KP_SOURCE, StandardCharsets.UTF_8);
            assertThat(md)
                    .contains("SettingsAssembler.java")
                    .contains("HooksAssembler.java")
                    .contains("SkillsAssembler.java")
                    .contains("CLAUDE.md");
        }

        @Test
        void kpSkillMd_providesAtLeastThreeCanonicalExamples() throws IOException {
            String md = Files.readString(KP_SOURCE, StandardCharsets.UTF_8);
            long exampleCount = md.lines()
                    .filter(line -> line.startsWith("### Example "))
                    .count();
            assertThat(exampleCount)
                    .as("story-0041-0001 DoD requires >= 3 canonical "
                            + "footprint examples")
                    .isGreaterThanOrEqualTo(3L);
        }
    }
}
