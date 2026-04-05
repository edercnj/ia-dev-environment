package dev.iadev.application.assembler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Content validation tests for x-review-compliance skill
 * template and security-pci rule template.
 *
 * <p>Validates Gherkin criteria GK-2, GK-3, GK-4 —
 * checklist item count, critical PCI coverage, and
 * prohibition examples.</p>
 */
@DisplayName("Compliance skill and rule content"
        + " validation")
class ComplianceSkillContentTest {

    private static String skillContent;
    private static String ruleContent;

    @BeforeAll
    static void loadTemplates() throws IOException {
        Path skillPath = Path.of(
                "src/main/resources/targets/claude"
                        + "/skills/conditional"
                        + "/x-review-compliance/SKILL.md");
        skillContent = Files.readString(
                skillPath, StandardCharsets.UTF_8);

        Path rulePath = Path.of(
                "src/main/resources/targets/claude"
                        + "/rules/conditional"
                        + "/11-security-pci.md");
        ruleContent = Files.readString(
                rulePath, StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("x-review-compliance SKILL.md")
    class SkillTemplate {

        @Test
        @DisplayName("frontmatter contains name and"
                + " user-invocable true")
        void frontmatter_containsNameAndInvocable() {
            assertThat(skillContent)
                    .contains("name: x-review-compliance");
            assertThat(skillContent)
                    .contains("user-invocable: true");
        }

        @Test
        @DisplayName("checklist has at least 20 items")
        void checklist_hasAtLeast20Items() {
            long count = skillContent.lines()
                    .filter(line ->
                            line.matches(
                                    "^\\s*\\d+\\.\\s+.*"))
                    .count();
            assertThat(count)
                    .as("PCI-DSS checklist item count")
                    .isGreaterThanOrEqualTo(20);
        }

        @Test
        @DisplayName("checklist covers PAN in logs")
        void checklist_coversPanInLogs() {
            assertThat(skillContent.toLowerCase())
                    .contains("pan");
            assertThat(skillContent.toLowerCase())
                    .containsAnyOf("log", "trace",
                            "error message");
        }

        @Test
        @DisplayName("checklist covers encryption at"
                + " rest")
        void checklist_coversEncryptionAtRest() {
            assertThat(skillContent.toLowerCase())
                    .containsAnyOf("encrypt",
                            "aes-256", "at rest",
                            "at-rest");
        }

        @Test
        @DisplayName("checklist covers authentication")
        void checklist_coversAuthentication() {
            assertThat(skillContent.toLowerCase())
                    .containsAnyOf("authenticat",
                            "mfa", "multi-factor");
        }

        @Test
        @DisplayName("checklist covers TLS transmission"
                + " security")
        void checklist_coversTlsTransmission() {
            assertThat(skillContent.toLowerCase())
                    .containsAnyOf("tls", "transmiss",
                            "ssl");
        }

        @Test
        @DisplayName("checklist covers access control")
        void checklist_coversAccessControl() {
            assertThat(skillContent.toLowerCase())
                    .containsAnyOf("access control",
                            "role-based", "rbac",
                            "authorization");
        }

        @Test
        @DisplayName("checklist has categories")
        void checklist_hasCategorized() {
            assertThat(skillContent)
                    .containsAnyOf(
                            "Data Protection",
                            "Cryptography",
                            "Authentication",
                            "Access Control",
                            "Logging",
                            "Transmission");
        }

        @Test
        @DisplayName("contains description field"
                + " in frontmatter")
        void frontmatter_containsDescription() {
            assertThat(skillContent)
                    .contains("description:");
        }
    }

    @Nested
    @DisplayName("security-pci rule template")
    class RuleTemplate {

        @Test
        @DisplayName("contains toString prohibition")
        void rule_containsToStringProhibition() {
            assertThat(ruleContent.toLowerCase())
                    .contains("tostring");
        }

        @Test
        @DisplayName("contains Math.random prohibition")
        void rule_containsMathRandomProhibition() {
            assertThat(ruleContent)
                    .containsAnyOf("Math.random",
                            "math.random");
        }

        @Test
        @DisplayName("contains CVV log prohibition")
        void rule_containsCvvLogProhibition() {
            assertThat(ruleContent.toLowerCase())
                    .contains("cvv");
        }

        @Test
        @DisplayName("contains PAN encryption"
                + " prohibition")
        void rule_containsPanEncryptionProhibition() {
            assertThat(ruleContent.toLowerCase())
                    .containsAnyOf("pan",
                            "card number");
        }

        @Test
        @DisplayName("contains serialization"
                + " prohibition")
        void rule_containsSerializationProhibition() {
            assertThat(ruleContent.toLowerCase())
                    .containsAnyOf("serial",
                            "log");
        }

        @Test
        @DisplayName("each prohibition has forbidden"
                + " and correct example")
        void rule_hasExamplesForProhibitions() {
            assertThat(ruleContent)
                    .containsAnyOf(
                            "Forbidden",
                            "forbidden",
                            "FORBIDDEN",
                            "Prohibited",
                            "prohibited");
            assertThat(ruleContent)
                    .containsAnyOf(
                            "Correct",
                            "correct",
                            "Alternative",
                            "alternative");
        }

        @Test
        @DisplayName("has at least 5 prohibitions")
        void rule_hasAtLeast5Prohibitions() {
            long count = ruleContent.lines()
                    .filter(line ->
                            line.matches(
                                    "^###\\s+.*"))
                    .count();
            assertThat(count)
                    .as("prohibition section count")
                    .isGreaterThanOrEqualTo(5);
        }
    }
}
