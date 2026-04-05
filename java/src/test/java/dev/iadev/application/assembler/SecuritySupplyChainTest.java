package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for story-0013-0021: Security KP extension with
 * SBOM/supply chain content and x-dependency-audit
 * extension with SBOM generation capabilities.
 */
@DisplayName("Security Supply Chain & SBOM Extensions")
class SecuritySupplyChainTest {

    @Nested
    @DisplayName("Security KP — Supply Chain Section")
    class SecurityKpSupplyChain {

        @Test
        @DisplayName("security KP contains Supply Chain"
                + " Security section")
        void assemble_securityKp_hasSupplyChainSection(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("## Supply Chain Security");
        }

        @Test
        @DisplayName("security KP contains SBOM Generation"
                + " subsection")
        void assemble_securityKp_hasSbomGeneration(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("### SBOM Generation");
        }

        @Test
        @DisplayName("security KP references CycloneDX and"
                + " SPDX formats")
        void assemble_securityKp_hasCycloneDxSpdx(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("CycloneDX")
                    .contains("SPDX");
        }

        @Test
        @DisplayName("security KP contains Artifact Signing"
                + " subsection")
        void assemble_securityKp_hasArtifactSigning(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("### Artifact Signing");
        }

        @Test
        @DisplayName("security KP contains SLSA Framework"
                + " subsection")
        void assemble_securityKp_hasSlsaFramework(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("### SLSA Framework");
        }

        @Test
        @DisplayName("security KP contains Dependency"
                + " Pinning subsection")
        void assemble_securityKp_hasDependencyPinning(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("### Dependency Pinning");
        }
    }

    @Nested
    @DisplayName("Security KP — SCA Section")
    class SecurityKpSca {

        @Test
        @DisplayName("security KP contains Software"
                + " Composition Analysis section")
        void assemble_securityKp_hasScaSection(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content).contains(
                    "## Software Composition Analysis");
        }

        @Test
        @DisplayName("security KP contains SCA Tools"
                + " subsection")
        void assemble_securityKp_hasScaTools(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("### SCA Tools");
        }

        @Test
        @DisplayName("security KP contains License"
                + " Compliance subsection")
        void assemble_securityKp_hasLicenseCompliance(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("### License Compliance");
        }

        @Test
        @DisplayName("security KP contains Transitive"
                + " Dependency Risk subsection")
        void assemble_securityKp_hasTransitiveRisk(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content).contains(
                    "### Transitive Dependency Risk");
        }

        @Test
        @DisplayName("security KP references SCA tools"
                + " by language")
        void assemble_securityKp_hasScaToolsByLanguage(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("Dependency-Check")
                    .contains("Snyk")
                    .contains("Grype");
        }
    }

    @Nested
    @DisplayName("Security KP — Reference Files")
    class SecurityKpReferences {

        @Test
        @DisplayName("sbom-generation-guide.md exists")
        void assemble_securityKp_sbomGuideExists(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            Path sbomGuide = tempDir.resolve(
                    "output/skills/security/references/"
                            + "sbom-generation-guide.md");
            assertThat(sbomGuide).exists();
        }

        @Test
        @DisplayName("sbom-generation-guide contains"
                + " CycloneDX vs SPDX comparison")
        void assemble_securityKp_sbomGuideHasComparison(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = Files.readString(
                    tempDir.resolve(
                            "output/skills/security/"
                                    + "references/"
                                    + "sbom-generation-guide"
                                    + ".md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("CycloneDX")
                    .contains("SPDX");
        }

        @Test
        @DisplayName("sbom-generation-guide contains"
                + " CI integration instructions")
        void assemble_securityKp_sbomGuideHasCi(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = Files.readString(
                    tempDir.resolve(
                            "output/skills/security/"
                                    + "references/"
                                    + "sbom-generation-guide"
                                    + ".md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("CI Integration");
        }

        @Test
        @DisplayName("supply-chain-hardening.md exists")
        void assemble_securityKp_hardeningGuideExists(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            Path guide = tempDir.resolve(
                    "output/skills/security/references/"
                            + "supply-chain-hardening.md");
            assertThat(guide).exists();
        }

        @Test
        @DisplayName("supply-chain-hardening contains"
                + " SLSA levels")
        void assemble_securityKp_hardeningHasSlsa(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = Files.readString(
                    tempDir.resolve(
                            "output/skills/security/"
                                    + "references/"
                                    + "supply-chain-hardening"
                                    + ".md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("SLSA")
                    .contains("Level 1")
                    .contains("Level 4");
        }

        @Test
        @DisplayName("supply-chain-hardening contains"
                + " Sigstore setup")
        void assemble_securityKp_hardeningHasSigstore(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = Files.readString(
                    tempDir.resolve(
                            "output/skills/security/"
                                    + "references/"
                                    + "supply-chain-hardening"
                                    + ".md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Sigstore")
                    .contains("cosign");
        }

        @Test
        @DisplayName("SKILL.md references new reference"
                + " files in table")
        void assemble_securityKp_skillMdRefsNewFiles(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains(
                            "sbom-generation-guide.md")
                    .contains(
                            "supply-chain-hardening.md");
        }
    }

    @Nested
    @DisplayName("Security KP — Backward Compatibility")
    class SecurityKpBackwardCompat {

        @Test
        @DisplayName("existing content preserved:"
                + " Purpose section")
        void assemble_securityKp_preservesPurpose(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("## Purpose")
                    .contains("comprehensive security"
                            + " guidelines");
        }

        @Test
        @DisplayName("existing content preserved:"
                + " Detailed References table")
        void assemble_securityKp_preservesRefsTable(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("## Detailed References")
                    .contains("security-principles.md")
                    .contains("application-security.md")
                    .contains("cryptography.md")
                    .contains("pentest-readiness.md");
        }

        @Test
        @DisplayName("existing content preserved:"
                + " frontmatter")
        void assemble_securityKp_preservesFrontmatter(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("name: security")
                    .contains("allowed-tools:");
        }
    }

    @Nested
    @DisplayName("x-dependency-audit — SBOM Extension")
    class DependencyAuditSbom {

        @Test
        @DisplayName("x-dependency-audit contains SBOM"
                + " Generation section")
        void assemble_depAudit_hasSbomSection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateDepAuditContent(tempDir);
            assertThat(content)
                    .contains("## SBOM Generation");
        }

        @Test
        @DisplayName("x-dependency-audit contains License"
                + " Attribution Report section")
        void assemble_depAudit_hasLicenseReport(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateDepAuditContent(tempDir);
            assertThat(content).contains(
                    "## License Attribution Report");
        }

        @Test
        @DisplayName("x-dependency-audit contains"
                + " Dependency Tree Visualization section")
        void assemble_depAudit_hasDependencyTree(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateDepAuditContent(tempDir);
            assertThat(content).contains(
                    "## Dependency Tree Visualization");
        }

        @Test
        @DisplayName("SBOM Generation references"
                + " CycloneDX JSON")
        void assemble_depAudit_sbomHasCycloneDx(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateDepAuditContent(tempDir);
            assertThat(content)
                    .contains("CycloneDX");
        }
    }

    @Nested
    @DisplayName("x-dependency-audit — Backward"
            + " Compatibility")
    class DependencyAuditBackwardCompat {

        @Test
        @DisplayName("existing content preserved:"
                + " Purpose section")
        void assemble_depAudit_preservesPurpose(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateDepAuditContent(tempDir);
            assertThat(content)
                    .contains("## Purpose")
                    .contains("Audits all dependencies");
        }

        @Test
        @DisplayName("existing content preserved:"
                + " Workflow section")
        void assemble_depAudit_preservesWorkflow(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateDepAuditContent(tempDir);
            assertThat(content)
                    .contains("## Workflow")
                    .contains("DETECT")
                    .contains("AUDIT")
                    .contains("PARSE")
                    .contains("CATEGORIZE")
                    .contains("REPORT");
        }

        @Test
        @DisplayName("existing content preserved:"
                + " Error Handling section")
        void assemble_depAudit_preservesErrorHandling(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateDepAuditContent(tempDir);
            assertThat(content)
                    .contains("## Error Handling")
                    .contains(
                            "Audit tool not installed");
        }

        @Test
        @DisplayName("existing content preserved:"
                + " frontmatter")
        void assemble_depAudit_preservesFrontmatter(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateDepAuditContent(tempDir);
            assertThat(content)
                    .contains("name: x-dependency-audit")
                    .contains("allowed-tools:");
        }
    }

    private String generateSecurityKpContent(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler =
                new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/security/SKILL.md"),
                StandardCharsets.UTF_8);
    }

    private String generateDepAuditContent(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler =
                new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/x-dependency-audit/"
                                + "SKILL.md"),
                StandardCharsets.UTF_8);
    }

    private void generateSecurityOutput(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler =
                new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
    }
}
