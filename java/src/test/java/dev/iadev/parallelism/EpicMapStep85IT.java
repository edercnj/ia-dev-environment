package dev.iadev.parallelism;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Story-0041-0005 integration validation: {@code x-epic-map} embeds
 * Step 8.5 "Parallelism Conflict Analysis" which invokes
 * {@code /x-parallel-eval --scope=epic}, and the implementation-map
 * template carries the matching section 8.5 placeholder.
 *
 * <p>The test validates three contracts:
 *
 * <ul>
 *   <li>Happy path — Step 8.5 is documented between Step 8 and Step 9
 *       in the skill source of truth.
 *   <li>Fail-open (RULE-006) — the SKILL.md documents the
 *       "analise pulada" marker used when {@code /x-parallel-eval}
 *       is missing or returns a non-zero exit code.
 *   <li>Template alignment — the shared template carries the section
 *       8.5 placeholder block so rendered maps can be populated.
 * </ul>
 *
 * <p>Running the actual {@code /x-epic-map} skill requires an LLM at
 * runtime, so this test is structural (golden-style): it asserts the
 * contracts the skill must implement, matching the pattern used by
 * {@link ParallelEvalSkillGoldenTest}.
 */
@DisplayName("x-epic-map Step 8.5 Parallelism Conflict Analysis")
class EpicMapStep85IT {

    private static final Path EPIC_MAP_SKILL = Path.of(
            "src", "main", "resources", "targets", "claude",
            "skills", "core", "plan", "x-epic-map",
            "SKILL.md");

    private static final Path MAP_TEMPLATE = Path.of(
            "src", "main", "resources", "shared", "templates",
            "_TEMPLATE-IMPLEMENTATION-MAP.md");

    @Test
    void skillMd_declaresStep8_5_betweenStep8AndStep9()
            throws IOException {
        String body = Files.readString(EPIC_MAP_SKILL);
        int step8 = body.indexOf(
                "### Step 8 — Task-Level Dependency Graph");
        int step85 = body.indexOf(
                "### Step 8.5 — Parallelism Conflict Analysis");
        int step9 = body.indexOf(
                "### Step 9 — Save and Report");

        assertThat(step8).isPositive();
        assertThat(step85).isGreaterThan(step8);
        assertThat(step9).isGreaterThan(step85);
    }

    @Test
    void skillMd_documentsParallelEvalInvocation()
            throws IOException {
        String body = Files.readString(EPIC_MAP_SKILL);
        assertThat(body).contains(
                "Skill(skill: \"x-parallel-eval\", "
                        + "args: \"--scope=epic");
    }

    @Test
    void skillMd_documentsFailOpenMarker()
            throws IOException {
        String body = Files.readString(EPIC_MAP_SKILL);
        assertThat(body).contains(
                "análise pulada — /x-parallel-eval "
                        + "não disponível");
        assertThat(body).contains("RULE-006");
    }

    @Test
    void skillMd_registersParallelismHeuristicsKnowledgePack()
            throws IOException {
        String body = Files.readString(EPIC_MAP_SKILL);
        assertThat(body).contains("parallelism-heuristics");
    }

    @Test
    void mapTemplate_carriesSection8_5Placeholder()
            throws IOException {
        String body = Files.readString(MAP_TEMPLATE);
        assertThat(body).contains(
                "## 8.5 Restrições de Paralelismo");
        assertThat(body).contains(
                "### 8.5.1 Pares Serializados Dentro da Fase");
        assertThat(body).contains(
                "### 8.5.2 Recomendação de Reagrupamento");
    }

    @Test
    void mapTemplate_documentsFailOpenContract()
            throws IOException {
        String body = Files.readString(MAP_TEMPLATE);
        assertThat(body).contains("RULE-006 fail-open");
        assertThat(body).contains("RULE-008");
    }
}
