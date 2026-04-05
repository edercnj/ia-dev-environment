package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for story-0022-0004: OWASP ASVS Reference
 * Knowledge Pack with L1/L2/L3 levels,
 * cross-reference tables, and 14 chapters (V1-V14).
 */
@DisplayName("OWASP ASVS Knowledge Pack")
class OwaspAsvsKpTest {

    @Nested
    @DisplayName("KnowledgePackSelection — owasp-asvs")
    class OwaspAsvsSelection {

        @Test
        @DisplayName("includes owasp-asvs when"
                + " frameworks contain owasp-asvs")
        void select_owaspAsvsFramework_includesPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .contains("owasp-asvs");
        }

        @Test
        @DisplayName("includes owasp-asvs when"
                + " frameworks has owasp-asvs among others")
        void select_multipleFrameworks_includesPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks(
                                    "owasp-asvs", "pci-dss")
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .contains("owasp-asvs");
        }

        @Test
        @DisplayName("excludes owasp-asvs when"
                + " frameworks is empty")
        void select_noFrameworks_excludesPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .doesNotContain("owasp-asvs");
        }

        @Test
        @DisplayName("excludes owasp-asvs when"
                + " frameworks has pci-dss only")
        void select_pciDssOnly_excludesPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .doesNotContain("owasp-asvs");
        }
    }

    @Nested
    @DisplayName("Rendering — no residual variables")
    class NoResidualVariables {

        @Test
        @DisplayName("rendered output has no template markers")
        void render_owaspAsvsKp_noResidualVariables(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "owasp-asvs/SKILL.md");
            assertThat(content)
                    .doesNotContain("{{")
                    .doesNotContain("}}")
                    .doesNotContain("{%");
        }
    }

    @Nested
    @DisplayName("ASVS Levels — L1/L2/L3 overview")
    class AsvsLevels {

        @Test
        @DisplayName("contains all three ASVS levels")
        void render_owaspAsvsKp_hasThreeLevels(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "owasp-asvs/SKILL.md");
            assertThat(content)
                    .contains("| L1 |")
                    .contains("| L2 |")
                    .contains("| L3 |");
        }
    }

    @Nested
    @DisplayName("All 14 ASVS chapters present")
    class FourteenChapters {

        @Test
        @DisplayName("references all chapters V1 through V14")
        void render_owaspAsvsKp_hasFourteenChapters(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String skill = readSkill(tempDir,
                    "owasp-asvs/SKILL.md");
            String v1v7 = readReference(tempDir,
                    "owasp-asvs/references/chapters-v1-v7.md");
            String v8v14 = readReference(tempDir,
                    "owasp-asvs/references/"
                            + "chapters-v8-v14.md");
            String combined =
                    skill + "\n" + v1v7 + "\n" + v8v14;

            for (int i = 1; i <= 14; i++) {
                assertThat(combined)
                        .as("Chapter V%d must be present", i)
                        .contains("V" + i);
            }
        }

        @Test
        @DisplayName("chapter summary table has 14 rows")
        void render_owaspAsvsKp_summaryHas14Rows(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "owasp-asvs/SKILL.md");

            long chapterRows = content.lines()
                    .filter(l -> l.matches(
                            "\\| V\\d+ \\|.*"))
                    .count();
            assertThat(chapterRows)
                    .as("Must have at least 14 chapter rows"
                            + " in summary table")
                    .isGreaterThanOrEqualTo(14);
        }
    }

    @Nested
    @DisplayName("Verification items have ID, level, CWE")
    class VerificationItems {

        @Test
        @DisplayName("V2 chapter has items with ASVS IDs")
        void render_v2Chapter_hasAsvsIds(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readReference(tempDir,
                    "owasp-asvs/references/"
                            + "chapters-v1-v7.md");

            assertThat(content)
                    .contains("V2.1.1")
                    .contains("V2.2.1");
        }

        @Test
        @DisplayName("items contain CWE references")
        void render_items_haveCweReferences(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String v1v7 = readReference(tempDir,
                    "owasp-asvs/references/"
                            + "chapters-v1-v7.md");
            String v8v14 = readReference(tempDir,
                    "owasp-asvs/references/"
                            + "chapters-v8-v14.md");
            String combined = v1v7 + "\n" + v8v14;

            Pattern cwePattern =
                    Pattern.compile("CWE-\\d+");
            Matcher matcher =
                    cwePattern.matcher(combined);
            long cweCount = 0;
            while (matcher.find()) {
                cweCount++;
            }
            assertThat(cweCount)
                    .as("Must contain CWE references")
                    .isGreaterThan(50);
        }

        @Test
        @DisplayName("items have ASVS levels L1 L2 or L3")
        void render_items_haveAsvsLevels(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String v1v7 = readReference(tempDir,
                    "owasp-asvs/references/"
                            + "chapters-v1-v7.md");

            Set<String> levels = v1v7.lines()
                    .filter(l -> l.startsWith("| V"))
                    .map(l -> extractLevel(l))
                    .filter(l -> !l.isEmpty())
                    .collect(Collectors.toSet());

            assertThat(levels)
                    .as("Must have L1 and L2 levels at"
                            + " minimum")
                    .contains("L1", "L2");
        }
    }

    @Nested
    @DisplayName("Cross-reference tables")
    class CrossReferences {

        @Test
        @DisplayName("OWASP Top 10 mapping present"
                + " with all 10 items")
        void render_owaspTop10_allTenMapped(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "owasp-asvs/SKILL.md");

            for (int i = 1; i <= 10; i++) {
                String id = String.format("A%02d", i);
                assertThat(content)
                        .as("OWASP Top 10 %s must be"
                                + " mapped", id)
                        .contains(id);
            }
        }

        @Test
        @DisplayName("NIST CSF has 5 functions mapped")
        void render_nistCsf_fiveFunctions(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "owasp-asvs/SKILL.md");

            assertThat(content)
                    .contains("Identify")
                    .contains("Protect")
                    .contains("Detect")
                    .contains("Respond")
                    .contains("Recover");
        }

        @Test
        @DisplayName("CIS Controls mapping present"
                + " with 18 controls")
        void render_cisControls_eighteenMapped(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "owasp-asvs/SKILL.md");

            long cisCount = content.lines()
                    .filter(l -> l.contains("CIS-"))
                    .count();
            assertThat(cisCount)
                    .as("Must have all 18 CIS controls")
                    .isGreaterThanOrEqualTo(18);
        }

        @Test
        @DisplayName("SANS Top 25 mapping present"
                + " with 25 CWEs")
        void render_sansTop25_twentyFiveMapped(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "owasp-asvs/SKILL.md");

            long sansCount = content.lines()
                    .filter(l -> l.startsWith("| ")
                            && l.contains("CWE-")
                            && !l.contains("---"))
                    .count();
            assertThat(sansCount)
                    .as("SANS Top 25 section must have"
                            + " >= 25 CWE entries")
                    .isGreaterThanOrEqualTo(25);
        }
    }

    @Nested
    @DisplayName("Frontmatter and structure")
    class FrontmatterAndStructure {

        @Test
        @DisplayName("has correct frontmatter name"
                + " and user-invocable false")
        void render_owaspAsvsKp_hasCorrectFrontmatter(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "owasp-asvs/SKILL.md");
            assertThat(content)
                    .contains("name: owasp-asvs")
                    .contains("user-invocable: false");
        }

        @Test
        @DisplayName("not generated when frameworks"
                + " do not include owasp-asvs")
        void render_noOwaspAsvs_notGenerated(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            Path kpDir = tempDir.resolve(
                    "skills/owasp-asvs");
            assertThat(kpDir).doesNotExist();
        }

        @Test
        @DisplayName("references directory is generated"
                + " with cross-references and chapters")
        void render_owaspAsvsKp_hasReferences(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp-asvs")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            Path refsDir = tempDir.resolve(
                    "skills/owasp-asvs/references");
            assertThat(refsDir).exists();
            assertThat(refsDir.resolve(
                    "cross-references.md")).exists();
            assertThat(refsDir.resolve(
                    "chapters-v1-v7.md")).exists();
            assertThat(refsDir.resolve(
                    "chapters-v8-v14.md")).exists();
        }
    }

    private String readSkill(Path outputDir, String path)
            throws IOException {
        return Files.readString(
                outputDir.resolve("skills/" + path),
                StandardCharsets.UTF_8);
    }

    private String readReference(
            Path outputDir, String path)
            throws IOException {
        return Files.readString(
                outputDir.resolve("skills/" + path),
                StandardCharsets.UTF_8);
    }

    private String extractLevel(String tableLine) {
        Pattern levelPattern =
                Pattern.compile("\\| (L[123]) \\|");
        Matcher matcher =
                levelPattern.matcher(tableLine);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}
