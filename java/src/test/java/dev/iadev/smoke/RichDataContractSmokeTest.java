package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that generated x-story-create SKILL.md files
 * contain rich data contract sections across all profiles.
 *
 * <p>Verifies the presence of:
 * <ul>
 *   <li>Request table with Tipo/Validacoes/Exemplo</li>
 *   <li>Response table with Sempre presente</li>
 *   <li>Error Codes Mapeados table (RFC 7807)</li>
 *   <li>Event Schema section with eventVersion</li>
 *   <li>Data Contract Precision Rules</li>
 * </ul>
 *
 * @see SmokeTestBase
 */
@DisplayName("RichDataContractSmokeTest")
class RichDataContractSmokeTest extends SmokeTestBase {

    private static final List<String> SKILL_PATHS = List.of(
            ".claude/skills/x-story-create/SKILL.md",
            ".codex/skills/x-story-create/SKILL.md",
            ".agents/skills/x-story-create/SKILL.md",
            ".github/skills/x-story-create/SKILL.md");

    @Nested
    @DisplayName("Request table format")
    class RequestTable {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke.SmokeProfiles#profiles")
        @DisplayName("Contains expanded Request table "
                + "with Tipo/M-O/Validacoes/Exemplo")
        void requestTable_allProfiles_hasRichColumns(
                String profile) throws IOException {
            runPipeline(profile);
            for (String skillPath : SKILL_PATHS) {
                String content = readSkill(
                        profile, skillPath);
                assertThat(content)
                        .as("Request table in %s for %s",
                                skillPath, profile)
                        .contains("##### 2.6.1 — Request Table")
                        .contains("| Campo | Tipo | M/O "
                                + "| Validacoes | Exemplo |");
            }
        }
    }

    @Nested
    @DisplayName("Response table format")
    class ResponseTable {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke.SmokeProfiles#profiles")
        @DisplayName("Contains Response table with "
                + "Sempre presente column")
        void responseTable_allProfiles_hasSemprePresenteCol(
                String profile) throws IOException {
            runPipeline(profile);
            for (String skillPath : SKILL_PATHS) {
                String content = readSkill(
                        profile, skillPath);
                assertThat(content)
                        .as("Response table in %s for %s",
                                skillPath, profile)
                        .contains("##### 2.6.2 — Response Table")
                        .contains("| Campo | Tipo "
                                + "| Sempre presente "
                                + "| Descricao |");
            }
        }
    }

    @Nested
    @DisplayName("Error Codes Mapeados")
    class ErrorCodes {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke.SmokeProfiles#profiles")
        @DisplayName("Contains Error Codes table "
                + "with RFC 7807 format")
        void errorCodes_allProfiles_hasRfc7807Table(
                String profile) throws IOException {
            runPipeline(profile);
            for (String skillPath : SKILL_PATHS) {
                String content = readSkill(
                        profile, skillPath);
                assertThat(content)
                        .as("Error Codes in %s for %s",
                                skillPath, profile)
                        .contains(
                                "##### 2.6.3 — Error Codes "
                                + "Mapeados")
                        .contains("| HTTP Status "
                                + "| Error Code "
                                + "| Condicao "
                                + "| Mensagem (RFC 7807) |")
                        .contains("RFC 7807");
            }
        }
    }

    @Nested
    @DisplayName("Event Schema section")
    class EventSchema {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke.SmokeProfiles#profiles")
        @DisplayName("Contains Event Schema section "
                + "with eventVersion field")
        void eventSchema_allProfiles_hasVersionField(
                String profile) throws IOException {
            runPipeline(profile);
            for (String skillPath : SKILL_PATHS) {
                String content = readSkill(
                        profile, skillPath);
                assertThat(content)
                        .as("Event Schema in %s for %s",
                                skillPath, profile)
                        .contains("##### 2.6.4 — Event Schema")
                        .contains("`eventVersion`")
                        .contains("Backward compatibility");
            }
        }
    }

    @Nested
    @DisplayName("Precision rules")
    class PrecisionRules {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke.SmokeProfiles#profiles")
        @DisplayName("Contains warning rule for "
                + "fields without type")
        void precisionRules_allProfiles_hasWarningRule(
                String profile) throws IOException {
            runPipeline(profile);
            for (String skillPath : SKILL_PATHS) {
                String content = readSkill(
                        profile, skillPath);
                assertThat(content)
                        .as("Precision rules in %s for %s",
                                skillPath, profile)
                        .contains("field type is required "
                                + "for rich contracts")
                        .contains("Nenhum endpoint "
                                + "declarado nesta story");
            }
        }
    }

    private String readSkill(
            String profile, String skillPath)
            throws IOException {
        Path file = getOutputDir(profile)
                .resolve(skillPath);
        assertThat(file)
                .as("Skill file must exist: %s", skillPath)
                .exists();
        return Files.readString(
                file, StandardCharsets.UTF_8);
    }
}
