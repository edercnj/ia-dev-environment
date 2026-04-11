package dev.iadev.application.assembler;

import dev.iadev.domain.model.SecurityConfig;
import dev.iadev.domain.model.SecurityConfig.ScanningConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Conditionally appends an "Automated Verification"
 * section to {@code 06-security-baseline.md} when at
 * least one scanning flag is enabled.
 *
 * <p>Each scanning flag controls which verification
 * mappings are included:
 * <ul>
 *   <li>{@code sast} — x-security-sast mappings
 *       (7 requirements)</li>
 *   <li>{@code secretScan} — x-security-secrets mappings
 *       (1 requirement)</li>
 *   <li>{@code dast} — x-security-dast and
 *       x-hardening-eval mappings
 *       (2 requirements: HTTP headers and TLS are
 *       verified via hardening evaluation triggered
 *       alongside DAST scanning)</li>
 * </ul>
 *
 * <p>When no scanning flag is active, the original file
 * is left unchanged (RULE-014 backward compatibility).</p>
 *
 * @see CoreRulesWriter
 * @see SecurityConfig
 */
public final class SecurityBaselineWriter {

    private static final String RULE_FILENAME =
            "06-security-baseline.md";

    private SecurityBaselineWriter() {
        // Utility class — no instantiation
    }

    /**
     * Appends the Automated Verification section to the
     * security baseline rule when scanning is enabled.
     *
     * @param securityConfig the security configuration
     * @param rulesDir the rules output directory
     * @return list of modified file paths (0 or 1)
     */
    static List<String> appendVerificationSection(
            SecurityConfig securityConfig,
            Path rulesDir) {
        if (!securityConfig.hasAnyScanning()) {
            return List.of();
        }

        Path ruleFile = rulesDir.resolve(RULE_FILENAME);
        if (!Files.exists(ruleFile)
                || !Files.isRegularFile(ruleFile)) {
            return List.of();
        }

        String existing = CopyHelpers.readFile(ruleFile);
        if (existing.contains(
                "## Automated Verification")) {
            return List.of();
        }
        String section = buildVerificationSection(
                securityConfig.scanning());
        CopyHelpers.writeFile(
                ruleFile, existing + section);
        return List.of(ruleFile.toString());
    }

    /**
     * Builds the Automated Verification markdown section
     * from the active scanning flags.
     *
     * @param scanning the scanning configuration
     * @return the markdown section string
     */
    static String buildVerificationSection(
            ScanningConfig scanning) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n## Automated Verification\n\n");
        sb.append("> This section is generated when "
                + "security scanning skills are "
                + "enabled.\n\n");
        sb.append("| Requirement | Verified By "
                + "| How to Run |\n");
        sb.append("| :--- | :--- | :--- |\n");

        List<String> rows = buildRows(scanning);
        for (String row : rows) {
            sb.append(row).append('\n');
        }

        return sb.toString();
    }

    private static List<String> buildRows(
            ScanningConfig scanning) {
        List<String> rows = new ArrayList<>();
        if (scanning.sast()) {
            rows.addAll(buildSastRows());
        }
        if (scanning.secretScan()) {
            rows.addAll(buildSecretScanRows());
        }
        if (scanning.dast()) {
            rows.addAll(buildDastRows());
        }
        return rows;
    }

    private static List<String> buildSastRows() {
        return List.of(
                "| Input deserialization "
                        + "| x-security-sast "
                        + "| `/x-security-sast --scope owasp` |",
                "| String escaping "
                        + "| x-security-sast "
                        + "| `/x-security-sast --scope owasp` |",
                "| Temp files/directories "
                        + "| x-security-sast "
                        + "| `/x-security-sast --scope owasp` |",
                "| Path operations "
                        + "| x-security-sast "
                        + "| `/x-security-sast --scope owasp` |",
                "| Error messages "
                        + "| x-security-sast "
                        + "| `/x-security-sast --scope owasp` |",
                "| Crypto RNG "
                        + "| x-security-sast "
                        + "| `/x-security-sast --scope owasp` |",
                "| Symlink following "
                        + "| x-security-sast "
                        + "| `/x-security-sast --scope owasp` |");
    }

    private static List<String> buildSecretScanRows() {
        return List.of(
                "| Hardcoded secrets/tokens/credentials "
                        + "| x-security-secrets "
                        + "| `/x-security-secrets` |");
    }

    private static List<String> buildDastRows() {
        return List.of(
                "| HTTP security headers "
                        + "| x-hardening-eval "
                        + "| `/x-hardening-eval` |",
                "| TLS configuration "
                        + "| x-hardening-eval "
                        + "| `/x-hardening-eval` |");
    }
}
