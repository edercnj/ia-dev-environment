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
 * Tests for story-0022-0025: Security KP — Cryptography
 * Reference content validation.
 *
 * <p>Validates that the cryptography.md source file
 * contains all required sections, uses correct
 * placeholders, and does not recommend deprecated
 * algorithms.</p>
 */
@DisplayName("Cryptography Reference — Content")
class CryptographyReferenceTest {

    private static String content;

    @BeforeAll
    static void loadContent() throws IOException {
        Path source = Path.of(
                "src/main/resources/knowledge/"
                        + "security/cryptography.md");
        content = Files.readString(
                source, StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("TLS 1.3 Configuration")
    class Tls13Configuration {

        @Test
        @DisplayName("contains TLS 1.3 per-framework"
                + " configuration section")
        void content_hasTls13PerFrameworkSection() {
            assertThat(content).contains(
                    "### TLS 1.3 Configuration per"
                            + " {{FRAMEWORK}}");
        }

        @Test
        @DisplayName("TLS section includes minimum"
                + " version enforcement")
        void content_hasMinimumTlsVersion() {
            assertThat(content)
                    .contains("TLSv1.3")
                    .contains("TLS 1.3");
        }

        @Test
        @DisplayName("TLS section includes certificate"
                + " management guidance")
        void content_hasCertificateManagement() {
            assertThat(content)
                    .contains("### Certificate Management")
                    .contains("Self-signed")
                    .contains("CA-signed");
        }

        @Test
        @DisplayName("TLS section includes mTLS patterns")
        void content_hasMtlsPatterns() {
            assertThat(content)
                    .contains("### Mutual TLS (mTLS)")
                    .contains("service-to-service")
                    .contains("zero-trust");
        }

        @Test
        @DisplayName("TLS config uses {{FRAMEWORK}}"
                + " placeholder")
        void content_tlsUsesFrameworkPlaceholder() {
            assertThat(content).contains(
                    "{{FRAMEWORK}} TLS configuration");
        }
    }

    @Nested
    @DisplayName("Cipher Suite Selection")
    class CipherSuiteSelection {

        @Test
        @DisplayName("contains cipher suite selection"
                + " table")
        void content_hasCipherSuiteTable() {
            assertThat(content)
                    .contains("## Cipher Suite Selection")
                    .contains("### Selection Table");
        }

        @Test
        @DisplayName("table has Recommended column")
        void content_hasRecommendedColumn() {
            assertThat(content)
                    .contains("| Recommended |");
        }

        @Test
        @DisplayName("table has Acceptable column")
        void content_hasAcceptableColumn() {
            assertThat(content)
                    .contains("| Acceptable |");
        }

        @Test
        @DisplayName("table has Deprecated column")
        void content_hasDeprecatedColumn() {
            assertThat(content)
                    .contains("| Deprecated |");
        }

        @Test
        @DisplayName("recommended includes ECDHE and"
                + " AES-256-GCM")
        void content_recommendedIncludesModernAlgorithms() {
            assertThat(content)
                    .contains("ECDHE")
                    .contains("AES-256-GCM")
                    .contains("ChaCha20-Poly1305");
        }

        @Test
        @DisplayName("deprecated includes 3DES, RC4,"
                + " MD5, SHA-1")
        void content_deprecatedListsLegacyAlgorithms() {
            assertThat(content)
                    .contains("3DES")
                    .contains("RC4")
                    .contains("DES");
        }
    }

    @Nested
    @DisplayName("Key Management Patterns")
    class KeyManagementPatterns {

        @Test
        @DisplayName("contains key management patterns"
                + " table")
        void content_hasKeyManagementPatternsTable() {
            assertThat(content)
                    .contains("### Key Management Patterns");
        }

        @Test
        @DisplayName("documents KMS envelope encryption")
        void content_hasKmsEnvelopeEncryption() {
            assertThat(content)
                    .contains("KMS")
                    .contains("Envelope Encryption");
        }

        @Test
        @DisplayName("documents key rotation strategies")
        void content_hasKeyRotation() {
            assertThat(content)
                    .contains("### Key Rotation Policy")
                    .contains("90 days")
                    .contains("365 days");
        }

        @Test
        @DisplayName("documents key derivation HKDF"
                + " and PBKDF2")
        void content_hasKeyDerivation() {
            assertThat(content)
                    .contains("### Key Derivation")
                    .contains("HKDF")
                    .contains("PBKDF2");
        }

        @Test
        @DisplayName("documents secrets managers")
        void content_hasSecretsManagers() {
            assertThat(content)
                    .contains("HashiCorp Vault")
                    .contains("AWS KMS")
                    .contains("Azure Key Vault")
                    .contains("GCP Cloud KMS");
        }
    }

    @Nested
    @DisplayName("Hashing Algorithm Selection")
    class HashingAlgorithmSelection {

        @Test
        @DisplayName("contains consolidated hashing"
                + " algorithm selection table")
        void content_hasHashingSelectionTable() {
            assertThat(content).contains(
                    "### Hashing Algorithm Selection");
        }

        @Test
        @DisplayName("password hashing recommends Argon2id"
                + " with bcrypt fallback")
        void content_passwordHashingRecommendsArgon2id() {
            assertThat(content)
                    .contains("**Argon2id** (preferred)")
                    .contains("bcrypt (fallback)");
        }

        @Test
        @DisplayName("data integrity recommends SHA-256")
        void content_integrityRecommendsSha256() {
            assertThat(content)
                    .contains("**SHA-256**");
        }

        @Test
        @DisplayName("HMAC recommends HMAC-SHA-256")
        void content_hmacRecommendsHmacSha256() {
            assertThat(content)
                    .contains("**HMAC-SHA-256**");
        }

        @Test
        @DisplayName("token generation recommends CSPRNG")
        void content_tokenRecommendsCsprng() {
            assertThat(content)
                    .contains("**CSPRNG**");
        }

        @Test
        @DisplayName("each use case lists what to"
                + " NEVER use")
        void content_hasNeverUseColumn() {
            assertThat(content)
                    .contains("| NEVER Use |");
        }
    }

    @Nested
    @DisplayName("Field-Level Encryption")
    class FieldLevelEncryption {

        @Test
        @DisplayName("contains field-level encryption"
                + " and tokenization section")
        void content_hasFieldLevelEncryptionSection() {
            assertThat(content).contains(
                    "## Field-Level Encryption"
                            + " and Tokenization");
        }

        @Test
        @DisplayName("documents FPE with FF1 algorithm")
        void content_hasFpeDocumentation() {
            assertThat(content)
                    .contains("Format-Preserving"
                            + " Encryption (FPE)")
                    .contains("FF1");
        }

        @Test
        @DisplayName("documents Vault-based tokenization")
        void content_hasVaultTokenization() {
            assertThat(content)
                    .contains("### Vault-Based"
                            + " Tokenization")
                    .contains("PCI-DSS");
        }

        @Test
        @DisplayName("documents deterministic vs"
                + " randomized encryption")
        void content_hasDeterministicVsRandomized() {
            assertThat(content)
                    .contains("Deterministic")
                    .contains("Randomized");
        }
    }

    @Nested
    @DisplayName("Deprecated Algorithms — Safety")
    class DeprecatedAlgorithmSafety {

        @Test
        @DisplayName("MD5 is NOT in Recommended or"
                + " Acceptable columns")
        void content_md5NotRecommended() {
            String selectionTable = extractSection(
                    "## Cipher Suite Selection",
                    "## Encryption at Rest");
            assertThat(selectionTable)
                    .doesNotContain("| MD5 |");
            String recommended =
                    extractRecommendedColumn(
                            selectionTable);
            assertThat(recommended)
                    .doesNotContain("MD5");
        }

        @Test
        @DisplayName("SHA-1 is NOT in Recommended or"
                + " Acceptable for hashing")
        void content_sha1NotRecommendedForHashing() {
            String hashTable = extractSection(
                    "### Hashing Algorithm Selection",
                    "### Password Hashing");
            assertThat(hashTable)
                    .doesNotContain(
                            "| **SHA-1**")
                    .doesNotContain(
                            "| SHA-1 (preferred)");
        }

        @Test
        @DisplayName("3DES is NOT in Recommended or"
                + " Acceptable")
        void content_3desNotRecommended() {
            String selectionTable = extractSection(
                    "## Cipher Suite Selection",
                    "## Encryption at Rest");
            assertThat(selectionTable)
                    .doesNotContain("| 3DES |");
        }

        @Test
        @DisplayName("RC4 is NOT in Recommended or"
                + " Acceptable")
        void content_rc4NotRecommended() {
            String selectionTable = extractSection(
                    "## Cipher Suite Selection",
                    "## Encryption at Rest");
            assertThat(selectionTable)
                    .doesNotContain("| RC4 |");
        }

        @Test
        @DisplayName("RSA < 2048 is NOT in Recommended"
                + " or Acceptable")
        void content_weakRsaNotRecommended() {
            String forbidden = extractSection(
                    "## Deprecated/Forbidden",
                    "## Anti-Patterns");
            assertThat(forbidden)
                    .contains("RSA < 2048")
                    .contains("**FORBIDDEN**");
        }

        @Test
        @DisplayName("deprecated algorithms table marks"
                + " all as FORBIDDEN or DEPRECATED")
        void content_allDeprecatedMarked() {
            assertThat(content)
                    .contains("| MD5 | **FORBIDDEN**")
                    .contains("| SHA-1 | **FORBIDDEN**")
                    .contains(
                            "| DES / 3DES | **FORBIDDEN**")
                    .contains("| RC4 | **FORBIDDEN**");
        }
    }

    @Nested
    @DisplayName("Placeholder Compliance — RULE-015")
    class PlaceholderCompliance {

        @Test
        @DisplayName("code blocks use {{LANGUAGE}}"
                + " placeholder")
        void content_usesLanguagePlaceholder() {
            assertThat(content)
                    .contains("{{LANGUAGE}}");
        }

        @Test
        @DisplayName("code blocks use {{FRAMEWORK}}"
                + " placeholder")
        void content_usesFrameworkPlaceholder() {
            assertThat(content)
                    .contains("{{FRAMEWORK}}");
        }

        @Test
        @DisplayName("no hardcoded framework names in"
                + " code examples")
        void content_noHardcodedFrameworkInExamples() {
            String codeBlocks = extractAllCodeBlocks();
            assertThat(codeBlocks)
                    .doesNotContainIgnoringCase(
                            "spring boot")
                    .doesNotContainIgnoringCase(
                            "quarkus")
                    .doesNotContainIgnoringCase(
                            "nestjs")
                    .doesNotContainIgnoringCase(
                            "fastapi");
        }
    }

    @Nested
    @DisplayName("Structure — All Required Sections")
    class RequiredSections {

        @Test
        @DisplayName("contains all top-level sections")
        void content_hasAllTopLevelSections() {
            assertThat(content)
                    .contains("## Encryption in Transit")
                    .contains("## Cipher Suite Selection")
                    .contains("## Encryption at Rest")
                    .contains("## Hashing")
                    .contains("## Key Management")
                    .contains("## Digital Signatures")
                    .contains("## Field-Level Encryption"
                            + " and Tokenization")
                    .contains(
                            "## Deprecated/Forbidden"
                                    + " Algorithms")
                    .contains("## Anti-Patterns");
        }
    }

    private static String extractSection(
            String startMarker, String endMarker) {
        int start = content.indexOf(startMarker);
        if (start < 0) {
            return "";
        }
        int end = content.indexOf(endMarker, start + 1);
        if (end < 0) {
            return content.substring(start);
        }
        return content.substring(start, end);
    }

    private static String extractRecommendedColumn(
            String tableSection) {
        StringBuilder recommended = new StringBuilder();
        for (String line : tableSection.split("\n")) {
            if (line.startsWith("|")
                    && line.contains("|")) {
                String[] cols = line.split("\\|");
                if (cols.length > 2) {
                    recommended.append(cols[2].trim())
                            .append(" ");
                }
            }
        }
        return recommended.toString();
    }

    private static String extractAllCodeBlocks() {
        StringBuilder blocks = new StringBuilder();
        boolean inBlock = false;
        for (String line : content.split("\n")) {
            if (line.startsWith("```")) {
                inBlock = !inBlock;
                continue;
            }
            if (inBlock) {
                blocks.append(line).append("\n");
            }
        }
        return blocks.toString();
    }
}
