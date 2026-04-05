package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SecurityConfig")
class SecurityConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("reads compliance key")
        void fromMap_withCompliance_listPopulated() {
            var map = Map.<String, Object>of(
                    "compliance",
                    List.of("pci-dss", "lgpd"));

            var result = SecurityConfig.fromMap(map);

            assertThat(result.frameworks())
                    .containsExactly("pci-dss", "lgpd");
        }

        @Test
        @DisplayName("falls back to frameworks key")
        void fromMap_withFrameworks_listPopulated() {
            var map = Map.<String, Object>of(
                    "frameworks",
                    List.of("spring-security", "oauth2"));

            var result = SecurityConfig.fromMap(map);

            assertThat(result.frameworks())
                    .containsExactly(
                            "spring-security", "oauth2");
        }

        @Test
        @DisplayName("compliance takes precedence")
        void fromMap_bothKeys_complianceWins() {
            var map = Map.<String, Object>of(
                    "compliance", List.of("pci-dss"),
                    "frameworks",
                    List.of("spring-security"));

            var result = SecurityConfig.fromMap(map);

            assertThat(result.frameworks())
                    .containsExactly("pci-dss");
        }

        @Test
        @DisplayName("empty map defaults to empty list")
        void fromMap_emptyMap_emptyList() {
            var result = SecurityConfig.fromMap(Map.of());

            assertThat(result.frameworks()).isEmpty();
        }

        @Test
        @DisplayName("empty map defaults scanning to all false")
        void fromMap_emptyMap_scanningAllFalse() {
            var result = SecurityConfig.fromMap(Map.of());

            var scanning = result.scanning();
            assertThat(scanning.sast()).isFalse();
            assertThat(scanning.dast()).isFalse();
            assertThat(scanning.secretScan()).isFalse();
            assertThat(scanning.containerScan()).isFalse();
            assertThat(scanning.infraScan()).isFalse();
        }

        @Test
        @DisplayName("empty map defaults qualityGate to none")
        void fromMap_emptyMap_qualityGateDefaults() {
            var result = SecurityConfig.fromMap(Map.of());

            var qg = result.qualityGate();
            assertThat(qg.provider()).isEqualTo("none");
            assertThat(qg.serverUrl()).isEmpty();
            assertThat(qg.qualityGate()).isEqualTo("default");
        }

        @Test
        @DisplayName("empty map defaults pentest to false")
        void fromMap_emptyMap_pentestFalse() {
            var result = SecurityConfig.fromMap(Map.of());

            assertThat(result.pentest()).isFalse();
            assertThat(result.pentestDefaultEnv())
                    .isEqualTo("local");
        }

        @Test
        @DisplayName("parses scanning sub-map with flags")
        void fromMap_withScanning_flagsParsed() {
            var scanningMap = Map.<String, Object>of(
                    "sast", true,
                    "dast", true,
                    "secretScan", true,
                    "containerScan", false,
                    "infraScan", true);
            var map = Map.<String, Object>of(
                    "scanning", scanningMap);

            var result = SecurityConfig.fromMap(map);

            assertThat(result.scanning().sast()).isTrue();
            assertThat(result.scanning().dast()).isTrue();
            assertThat(result.scanning().secretScan()).isTrue();
            assertThat(result.scanning().containerScan())
                    .isFalse();
            assertThat(result.scanning().infraScan()).isTrue();
        }

        @Test
        @DisplayName("parses qualityGate sub-map")
        void fromMap_withQualityGate_fieldsParsed() {
            var qgMap = Map.<String, Object>of(
                    "provider", "sonarqube",
                    "serverUrl",
                    "https://sonar.example.com",
                    "qualityGate", "Sonar way");
            var map = Map.<String, Object>of(
                    "qualityGate", qgMap);

            var result = SecurityConfig.fromMap(map);

            assertThat(result.qualityGate().provider())
                    .isEqualTo("sonarqube");
            assertThat(result.qualityGate().serverUrl())
                    .isEqualTo("https://sonar.example.com");
            assertThat(result.qualityGate().qualityGate())
                    .isEqualTo("Sonar way");
        }

        @Test
        @DisplayName("parses pentest fields")
        void fromMap_withPentest_fieldsParsed() {
            var map = Map.<String, Object>of(
                    "pentest", true,
                    "pentestDefaultEnv", "dev");

            var result = SecurityConfig.fromMap(map);

            assertThat(result.pentest()).isTrue();
            assertThat(result.pentestDefaultEnv())
                    .isEqualTo("dev");
        }

        @Test
        @DisplayName("parses all fields together")
        void fromMap_allFields_fullConfig() {
            var scanningMap = Map.<String, Object>of(
                    "sast", true,
                    "dast", true,
                    "secretScan", true,
                    "containerScan", true,
                    "infraScan", true);
            var qgMap = Map.<String, Object>of(
                    "provider", "sonarcloud",
                    "serverUrl",
                    "https://sonarcloud.io",
                    "qualityGate", "default");
            Map<String, Object> map = new HashMap<>();
            map.put("compliance",
                    List.of("pci-dss"));
            map.put("scanning", scanningMap);
            map.put("qualityGate", qgMap);
            map.put("pentest", true);
            map.put("pentestDefaultEnv", "homolog");

            var result = SecurityConfig.fromMap(map);

            assertThat(result.frameworks())
                    .containsExactly("pci-dss");
            assertThat(result.scanning().sast()).isTrue();
            assertThat(result.qualityGate().provider())
                    .isEqualTo("sonarcloud");
            assertThat(result.pentest()).isTrue();
            assertThat(result.pentestDefaultEnv())
                    .isEqualTo("homolog");
        }
    }

    @Test
    @DisplayName("frameworks list is immutable")
    void frameworks_immutable_throwsOnModification() {
        var mutableList = new ArrayList<>(
                List.of("keycloak"));
        var config = new SecurityConfig(
                mutableList,
                SecurityConfig.ScanningConfig.defaults(),
                SecurityConfig.QualityGateConfig.defaults(),
                false, "local");

        assertThatThrownBy(
                () -> config.frameworks().add("oauth2"))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("defensive copy prevents mutation"
            + " via original list")
    void frameworks_defensiveCopy_originalMutationIgnored() {
        var mutableList = new ArrayList<>(
                List.of("keycloak"));
        var config = new SecurityConfig(
                mutableList,
                SecurityConfig.ScanningConfig.defaults(),
                SecurityConfig.QualityGateConfig.defaults(),
                false, "local");
        mutableList.add("oauth2");

        assertThat(config.frameworks())
                .containsExactly("keycloak");
    }

    @Nested
    @DisplayName("ScanningConfig")
    class ScanningConfigTest {

        @Test
        @DisplayName("defaults returns all false")
        void defaults_allFalse() {
            var scanning =
                    SecurityConfig.ScanningConfig.defaults();

            assertThat(scanning.sast()).isFalse();
            assertThat(scanning.dast()).isFalse();
            assertThat(scanning.secretScan()).isFalse();
            assertThat(scanning.containerScan()).isFalse();
            assertThat(scanning.infraScan()).isFalse();
        }

        @Test
        @DisplayName("fromMap with empty map returns defaults")
        void fromMap_emptyMap_returnsDefaults() {
            var scanning =
                    SecurityConfig.ScanningConfig
                            .fromMap(Map.of());

            assertThat(scanning.sast()).isFalse();
            assertThat(scanning.dast()).isFalse();
            assertThat(scanning.secretScan()).isFalse();
            assertThat(scanning.containerScan()).isFalse();
            assertThat(scanning.infraScan()).isFalse();
        }

        @Test
        @DisplayName("fromMap parses individual flags")
        void fromMap_individualFlags_parsed() {
            var map = Map.<String, Object>of(
                    "sast", true,
                    "containerScan", true);

            var scanning =
                    SecurityConfig.ScanningConfig
                            .fromMap(map);

            assertThat(scanning.sast()).isTrue();
            assertThat(scanning.dast()).isFalse();
            assertThat(scanning.secretScan()).isFalse();
            assertThat(scanning.containerScan()).isTrue();
            assertThat(scanning.infraScan()).isFalse();
        }

        @Test
        @DisplayName("fromMap with all true parses correctly")
        void fromMap_allTrue_parsed() {
            var map = Map.<String, Object>of(
                    "sast", true,
                    "dast", true,
                    "secretScan", true,
                    "containerScan", true,
                    "infraScan", true);

            var scanning =
                    SecurityConfig.ScanningConfig
                            .fromMap(map);

            assertThat(scanning.sast()).isTrue();
            assertThat(scanning.dast()).isTrue();
            assertThat(scanning.secretScan()).isTrue();
            assertThat(scanning.containerScan()).isTrue();
            assertThat(scanning.infraScan()).isTrue();
        }
    }

    @Nested
    @DisplayName("QualityGateConfig")
    class QualityGateConfigTest {

        @Test
        @DisplayName("defaults returns provider none")
        void defaults_providerNone() {
            var qg = SecurityConfig.QualityGateConfig
                    .defaults();

            assertThat(qg.provider()).isEqualTo("none");
            assertThat(qg.serverUrl()).isEmpty();
            assertThat(qg.qualityGate())
                    .isEqualTo("default");
        }

        @Test
        @DisplayName("fromMap with empty map returns defaults")
        void fromMap_emptyMap_returnsDefaults() {
            var qg = SecurityConfig.QualityGateConfig
                    .fromMap(Map.of());

            assertThat(qg.provider()).isEqualTo("none");
            assertThat(qg.serverUrl()).isEmpty();
            assertThat(qg.qualityGate())
                    .isEqualTo("default");
        }

        @Test
        @DisplayName("fromMap with sonarqube parses fields")
        void fromMap_sonarqube_fieldsParsed() {
            var map = Map.<String, Object>of(
                    "provider", "sonarqube",
                    "serverUrl",
                    "https://sonar.example.com",
                    "qualityGate", "Sonar way");

            var qg = SecurityConfig.QualityGateConfig
                    .fromMap(map);

            assertThat(qg.provider())
                    .isEqualTo("sonarqube");
            assertThat(qg.serverUrl())
                    .isEqualTo("https://sonar.example.com");
            assertThat(qg.qualityGate())
                    .isEqualTo("Sonar way");
        }

        @Test
        @DisplayName("fromMap with provider only uses defaults"
                + " for other fields")
        void fromMap_providerOnly_otherFieldsDefault() {
            var map = Map.<String, Object>of(
                    "provider", "sonarcloud");

            var qg = SecurityConfig.QualityGateConfig
                    .fromMap(map);

            assertThat(qg.provider())
                    .isEqualTo("sonarcloud");
            assertThat(qg.serverUrl()).isEmpty();
            assertThat(qg.qualityGate())
                    .isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("pentest flag")
    class PentestFlag {

        @Test
        @DisplayName("pentest defaults to false from empty map")
        void fromMap_emptyMap_pentestFalse() {
            var result = SecurityConfig.fromMap(Map.of());

            assertThat(result.pentest()).isFalse();
        }

        @Test
        @DisplayName("pentest true when pentest is true")
        void fromMap_pentestTrue_pentestTrue() {
            var map = Map.<String, Object>of(
                    "pentest", true);

            var result = SecurityConfig.fromMap(map);

            assertThat(result.pentest()).isTrue();
        }

        @Test
        @DisplayName("pentest false when pentest is false")
        void fromMap_pentestFalse_pentestFalse() {
            var map = Map.<String, Object>of(
                    "pentest", false);

            var result = SecurityConfig.fromMap(map);

            assertThat(result.pentest()).isFalse();
        }
    }

    @Test
    @DisplayName("single-arg constructor defaults scanning"
            + " to disabled")
    void constructor_singleArg_scanningDisabled() {
        var config = new SecurityConfig(List.of());

        assertThat(config.scanning())
                .isEqualTo(SecurityConfig.ScanningConfig.DISABLED);
        assertThat(config.scanning().containerScan())
                .isFalse();
    }

    @Nested
    @DisplayName("ScanningConfig")
    class ScanningConfigTests {

        @Test
        @DisplayName("fromMap with containerScan true")
        void fromMap_containerScanTrue_parsed() {
            var map = Map.<String, Object>of(
                    "containerScan", true);

            var result =
                    SecurityConfig.ScanningConfig.fromMap(map);

            assertThat(result.containerScan()).isTrue();
        }

        @Test
        @DisplayName("fromMap with containerScan false")
        void fromMap_containerScanFalse_parsed() {
            var map = Map.<String, Object>of(
                    "containerScan", false);

            var result =
                    SecurityConfig.ScanningConfig.fromMap(map);

            assertThat(result.containerScan()).isFalse();
        }

        @Test
        @DisplayName("fromMap with empty map defaults to"
                + " false")
        void fromMap_emptyMap_defaultsFalse() {
            var result =
                    SecurityConfig.ScanningConfig.fromMap(
                            Map.of());

            assertThat(result.containerScan()).isFalse();
        }

        @Test
        @DisplayName("DISABLED constant has containerScan"
                + " false")
        void disabled_containerScan_isFalse() {
            assertThat(SecurityConfig.ScanningConfig.DISABLED
                    .containerScan()).isFalse();
        }
    }

    @Nested
    @DisplayName("fromMap with scanning section")
    class FromMapWithScanning {

        @Test
        @DisplayName("parses scanning.containerScan from"
                + " nested map")
        void fromMap_scanningSection_containerScanParsed() {
            var map = Map.<String, Object>of(
                    "scanning",
                    Map.of("containerScan", true));

            var result = SecurityConfig.fromMap(map);

            assertThat(result.scanning().containerScan())
                    .isTrue();
        }

        @Test
        @DisplayName("missing scanning section defaults to"
                + " disabled")
        void fromMap_noScanningSection_defaultsDisabled() {
            var result = SecurityConfig.fromMap(Map.of());

            assertThat(result.scanning().containerScan())
                    .isFalse();
        }

        @Test
        @DisplayName("compliance and scanning coexist")
        void fromMap_complianceAndScanning_bothParsed() {
            var map = Map.<String, Object>of(
                    "compliance",
                    List.of("pci-dss"),
                    "scanning",
                    Map.of("containerScan", true));

            var result = SecurityConfig.fromMap(map);

            assertThat(result.frameworks())
                    .containsExactly("pci-dss");
            assertThat(result.scanning().containerScan())
                    .isTrue();
        }
    }
}
