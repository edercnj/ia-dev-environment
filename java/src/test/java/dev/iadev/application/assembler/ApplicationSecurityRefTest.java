package dev.iadev.application.assembler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for story-0022-0024: Application Security
 * Reference — validates the application-security.md
 * knowledge pack covers all OWASP Top 10 categories,
 * SANS Top 25 cross-references, tool recommendation
 * table, and uses {{LANGUAGE}} placeholders.
 */
@DisplayName("Application Security Reference KP")
class ApplicationSecurityRefTest {

    private static String content;

    @BeforeAll
    static void loadContent() throws IOException {
        try (InputStream is = ApplicationSecurityRefTest
                .class.getClassLoader()
                .getResourceAsStream(
                        "knowledge/security/"
                                + "application-security"
                                + ".md")) {
            assertThat(is).isNotNull();
            content = new String(
                    is.readAllBytes(),
                    StandardCharsets.UTF_8);
        }
    }

    @Nested
    @DisplayName("OWASP Top 10 Categories")
    class OwaspCategories {

        @Test
        @DisplayName("A01 Broken Access Control present")
        void content_hasA01() {
            assertThat(content).contains(
                    "### A01:2021 "
                            + "— Broken Access Control");
        }

        @Test
        @DisplayName("A02 Cryptographic Failures present")
        void content_hasA02() {
            assertThat(content).contains(
                    "### A02:2021 "
                            + "— Cryptographic Failures");
        }

        @Test
        @DisplayName("A03 Injection present")
        void content_hasA03() {
            assertThat(content).contains(
                    "### A03:2021 — Injection");
        }

        @Test
        @DisplayName("A04 Insecure Design present")
        void content_hasA04() {
            assertThat(content).contains(
                    "### A04:2021 — Insecure Design");
        }

        @Test
        @DisplayName("A05 Security Misconfiguration"
                + " present")
        void content_hasA05() {
            assertThat(content).contains(
                    "### A05:2021 "
                            + "— Security Misconfiguration");
        }

        @Test
        @DisplayName("A06 Vulnerable Components present")
        void content_hasA06() {
            assertThat(content).contains(
                    "### A06:2021 "
                            + "— Vulnerable and Outdated");
        }

        @Test
        @DisplayName("A07 Auth Failures present")
        void content_hasA07() {
            assertThat(content).contains(
                    "### A07:2021 "
                            + "— Identification and");
        }

        @Test
        @DisplayName("A08 Integrity Failures present")
        void content_hasA08() {
            assertThat(content).contains(
                    "### A08:2021 "
                            + "— Software and Data");
        }

        @Test
        @DisplayName("A09 Logging Failures present")
        void content_hasA09() {
            assertThat(content).contains(
                    "### A09:2021 "
                            + "— Security Logging");
        }

        @Test
        @DisplayName("A10 SSRF present")
        void content_hasA10() {
            assertThat(content).contains(
                    "### A10:2021 — Server-Side Request");
        }
    }

    @Nested
    @DisplayName("Category Structure")
    class CategoryStructure {

        @Test
        @DisplayName("each category has Description")
        void content_eachCategoryHasDescription() {
            assertThat(
                    countOccurrences(
                            content,
                            "**Description:**"))
                    .isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("each category has Impact CIA")
        void content_eachCategoryHasImpact() {
            assertThat(
                    countOccurrences(
                            content,
                            "**Impact:**"))
                    .isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("each category has Associated CWEs")
        void content_eachCategoryHasCwes() {
            assertThat(
                    countOccurrences(
                            content,
                            "**Associated CWEs:**"))
                    .isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("each category has Detection"
                + " Patterns")
        void content_eachCategoryHasDetection() {
            assertThat(
                    countOccurrences(
                            content,
                            "**Detection Patterns:**"))
                    .isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("each category has SANS cross-ref")
        void content_eachCategoryHasSansCrossRef() {
            assertThat(
                    countOccurrences(
                            content,
                            "**SANS Top 25 "
                                    + "Cross-Reference:**"))
                    .isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("impact uses CIA triad format")
        void content_impactUsesCiaTriad() {
            assertThat(content)
                    .contains("Confidentiality")
                    .contains("Integrity")
                    .contains("Availability");
        }
    }

    @Nested
    @DisplayName("{{LANGUAGE}} Placeholders")
    class LanguagePlaceholders {

        @Test
        @DisplayName("code blocks use LANGUAGE placeholder")
        void content_codeBlocksUseLanguagePlaceholder() {
            assertThat(content)
                    .contains("```{{LANGUAGE}}");
        }

        @Test
        @DisplayName("no hardcoded language in code blocks")
        void content_noHardcodedLanguageInBlocks() {
            assertThat(content)
                    .doesNotContain("```java")
                    .doesNotContain("```python")
                    .doesNotContain("```go\n")
                    .doesNotContain("```rust")
                    .doesNotContain("```typescript")
                    .doesNotContain("```kotlin");
        }

        @Test
        @DisplayName("multiple code blocks with"
                + " LANGUAGE placeholder")
        void content_multipleLanguageBlocks() {
            assertThat(
                    countOccurrences(
                            content,
                            "```{{LANGUAGE}}"))
                    .isGreaterThanOrEqualTo(5);
        }
    }

    @Nested
    @DisplayName("SANS Top 25 Cross-Reference Table")
    class SansCrossRefTable {

        @Test
        @DisplayName("cross-reference table present")
        void content_hasCrossRefTable() {
            assertThat(content).contains(
                    "## OWASP-to-SANS Top 25 "
                            + "Cross-Reference Table");
        }

        @Test
        @DisplayName("table maps CWE IDs")
        void content_tableMapsCweIds() {
            assertThat(content)
                    .contains("CWE-89")
                    .contains("CWE-79")
                    .contains("CWE-78")
                    .contains("CWE-862")
                    .contains("CWE-863")
                    .contains("CWE-22")
                    .contains("CWE-502")
                    .contains("CWE-918")
                    .contains("CWE-287")
                    .contains("CWE-798")
                    .contains("CWE-327")
                    .contains("CWE-352")
                    .contains("CWE-307")
                    .contains("CWE-778")
                    .contains("CWE-532");
        }

        @Test
        @DisplayName("at least 15 SANS CWEs mapped")
        void content_atLeast15SansCwesMapped() {
            int mapped = 0;
            String[] sansCwes = {
                    "CWE-787", "CWE-79", "CWE-89",
                    "CWE-416", "CWE-78", "CWE-20",
                    "CWE-125", "CWE-22", "CWE-352",
                    "CWE-434", "CWE-862", "CWE-476",
                    "CWE-287", "CWE-190", "CWE-502",
                    "CWE-77", "CWE-119", "CWE-798",
                    "CWE-918", "CWE-306", "CWE-362",
                    "CWE-269", "CWE-94", "CWE-863",
                    "CWE-276"
            };
            for (String cwe : sansCwes) {
                if (content.contains(cwe)) {
                    mapped++;
                }
            }
            assertThat(mapped)
                    .as("SANS Top 25 CWEs mapped")
                    .isGreaterThanOrEqualTo(15);
        }
    }

    @Nested
    @DisplayName("Tool Recommendation Table")
    class ToolRecommendation {

        @Test
        @DisplayName("tool table present")
        void content_hasToolTable() {
            assertThat(content).contains(
                    "## Tool Recommendation "
                            + "by Vulnerability Type");
        }

        @Test
        @DisplayName("table has tool types")
        void content_tableHasToolTypes() {
            assertThat(content)
                    .contains("SAST")
                    .contains("DAST")
                    .contains("SCA")
                    .contains("Manual Review");
        }

        @Test
        @DisplayName("table has effectiveness ratings")
        void content_tableHasEffectivenessRatings() {
            assertThat(content)
                    .contains("High")
                    .contains("Medium-High")
                    .contains("Medium");
        }

        @Test
        @DisplayName("table covers major vuln types")
        void content_tableCoversMajorVulnTypes() {
            assertThat(content)
                    .contains("Injection")
                    .contains("Auth/Access Control")
                    .contains("Cryptographic Failures")
                    .contains("Insecure Design")
                    .contains("Misconfiguration")
                    .contains("Vulnerable Components")
                    .contains("Data Integrity")
                    .contains("Logging Failures")
                    .contains("SSRF");
        }
    }

    @Nested
    @DisplayName("Backward Compatibility")
    class BackwardCompat {

        @Test
        @DisplayName("Security Headers section preserved")
        void content_hasSecurityHeaders() {
            assertThat(content)
                    .contains("## Security Headers");
        }

        @Test
        @DisplayName("Secrets Management section preserved")
        void content_hasSecretsManagement() {
            assertThat(content)
                    .contains("## Secrets Management");
        }

        @Test
        @DisplayName("Input Validation section preserved")
        void content_hasInputValidation() {
            assertThat(content)
                    .contains("## Input Validation "
                            + "Framework");
        }

        @Test
        @DisplayName("Dependency Security section"
                + " preserved")
        void content_hasDependencySecurity() {
            assertThat(content)
                    .contains("## Dependency Security");
        }

        @Test
        @DisplayName("Anti-Patterns section preserved")
        void content_hasAntiPatterns() {
            assertThat(content)
                    .contains("## Anti-Patterns "
                            + "(FORBIDDEN)");
        }

        @Test
        @DisplayName("CVE Response Policy preserved")
        void content_hasCveResponsePolicy() {
            assertThat(content)
                    .contains("### CVE Response Policy");
        }
    }

    @Nested
    @DisplayName("Session Management")
    class SessionManagement {

        @Test
        @DisplayName("session management section present")
        void content_hasSessionManagement() {
            assertThat(content)
                    .contains("## Session Management");
        }

        @Test
        @DisplayName("session security attributes covered")
        void content_hasSessionSecurityAttrs() {
            assertThat(content)
                    .contains("Secure")
                    .contains("HttpOnly")
                    .contains("SameSite");
        }
    }

    @Nested
    @DisplayName("Output Encoding")
    class OutputEncoding {

        @Test
        @DisplayName("output encoding section present")
        void content_hasOutputEncoding() {
            assertThat(content)
                    .contains("## Output Encoding");
        }

        @Test
        @DisplayName("encoding contexts covered")
        void content_hasEncodingContexts() {
            assertThat(content)
                    .contains("HTML body")
                    .contains("HTML attribute")
                    .contains("JavaScript")
                    .contains("URL parameter")
                    .contains("CSS")
                    .contains("JSON");
        }
    }

    private static int countOccurrences(
            String text, String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(
                substring, idx)) != -1) {
            count++;
            idx += substring.length();
        }
        return count;
    }
}
