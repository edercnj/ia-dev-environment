package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Represents the security configuration section.
 *
 * <p>Contains a list of compliance framework names (e.g.,
 * pci-dss, lgpd, sox, hipaa). Defaults to an empty list.
 * The YAML key {@code compliance} is read; the legacy
 * {@code frameworks} key is supported as fallback.</p>
 *
 * <p>Sub-records {@link ScanningConfig} and
 * {@link QualityGateConfig} group scanning flags and
 * quality gate settings respectively.</p>
 *
 * @param frameworks the list of compliance framework
 *     names (default: empty, immutable)
 * @param scanning the scanning configuration
 * @param qualityGate the quality gate configuration
 * @param pentest whether pentest is enabled
 * @param pentestDefaultEnv the default pentest environment
 */
public record SecurityConfig(
        List<String> frameworks,
        ScanningConfig scanning,
        QualityGateConfig qualityGate,
        boolean pentest,
        String pentestDefaultEnv) {

    private static final String DEFAULT_PENTEST_ENV = "local";

    /**
     * Scanning tool flags for security analysis.
     *
     * @param sast static application security testing
     * @param dast dynamic application security testing
     * @param secretScan secret detection in source code
     * @param containerScan container image scanning
     * @param infraScan infrastructure-as-code scanning
     */
    public record ScanningConfig(
            boolean sast,
            boolean dast,
            boolean secretScan,
            boolean containerScan,
            boolean infraScan) {

        /**
         * Returns a ScanningConfig with all flags false.
         *
         * @return default ScanningConfig
         */
        public static ScanningConfig defaults() {
            return new ScanningConfig(
                    false, false, false, false, false);
        }

        /**
         * Creates a ScanningConfig from a YAML-parsed map.
         *
         * @param map the map from YAML deserialization
         * @return a new ScanningConfig instance
         */
        public static ScanningConfig fromMap(
                Map<String, Object> map) {
            return new ScanningConfig(
                    MapHelper.optionalBoolean(
                            map, "sast", false),
                    MapHelper.optionalBoolean(
                            map, "dast", false),
                    MapHelper.optionalBoolean(
                            map, "secretScan", false),
                    MapHelper.optionalBoolean(
                            map, "containerScan", false),
                    MapHelper.optionalBoolean(
                            map, "infraScan", false));
        }
    }

    /**
     * Quality gate configuration for code analysis.
     *
     * @param provider the provider name (none, sonarqube,
     *        sonarcloud)
     * @param serverUrl the server URL (empty when provider
     *        is none)
     * @param qualityGate the quality gate profile name
     */
    public record QualityGateConfig(
            String provider,
            String serverUrl,
            String qualityGate) {

        private static final String DEFAULT_PROVIDER = "none";
        private static final String DEFAULT_QUALITY_GATE =
                "default";

        /**
         * Returns a QualityGateConfig with safe defaults.
         *
         * @return default QualityGateConfig
         */
        public static QualityGateConfig defaults() {
            return new QualityGateConfig(
                    DEFAULT_PROVIDER, "",
                    DEFAULT_QUALITY_GATE);
        }

        /**
         * Creates a QualityGateConfig from a YAML-parsed map.
         *
         * @param map the map from YAML deserialization
         * @return a new QualityGateConfig instance
         */
        public static QualityGateConfig fromMap(
                Map<String, Object> map) {
            return new QualityGateConfig(
                    MapHelper.optionalString(
                            map, "provider",
                            DEFAULT_PROVIDER),
                    MapHelper.optionalString(
                            map, "serverUrl", ""),
                    MapHelper.optionalString(
                            map, "qualityGate",
                            DEFAULT_QUALITY_GATE));
        }
    }

    /**
     * Compact constructor enforcing immutability.
     */
    public SecurityConfig {
        frameworks = List.copyOf(frameworks);
    }

    /**
     * Creates a SecurityConfig from a YAML-parsed map.
     *
     * <p>Reads the {@code compliance} key first; falls
     * back to {@code frameworks} for backward
     * compatibility. Parses sub-records for scanning
     * and quality gate from nested maps.</p>
     *
     * @param map the map from YAML deserialization
     * @return a new SecurityConfig instance
     */
    public static SecurityConfig fromMap(
            Map<String, Object> map) {
        List<String> values = parseFrameworks(map);
        ScanningConfig scanning = ScanningConfig.fromMap(
                MapHelper.optionalMap(map, "scanning"));
        QualityGateConfig qualityGate =
                QualityGateConfig.fromMap(
                        MapHelper.optionalMap(
                                map, "qualityGate"));
        boolean pentest = MapHelper.optionalBoolean(
                map, "pentest", false);
        String pentestDefaultEnv = MapHelper.optionalString(
                map, "pentestDefaultEnv",
                DEFAULT_PENTEST_ENV);
        return new SecurityConfig(
                values, scanning, qualityGate,
                pentest, pentestDefaultEnv);
    }

    private static List<String> parseFrameworks(
            Map<String, Object> map) {
        if (map.containsKey("compliance")) {
            return MapHelper.optionalStringList(
                    map, "compliance");
        }
        return MapHelper.optionalStringList(
                map, "frameworks");
    }
}
