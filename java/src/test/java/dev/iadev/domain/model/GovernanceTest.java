package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Governance")
class GovernanceTest {

    @Nested
    @DisplayName("compact constructor")
    class CompactConstructor {

        @Test
        @DisplayName("builds with all fields populated")
        void ctor_allFields_allSet() {
            Set<Platform> platforms = Set.of(Platform.CLAUDE_CODE);

            Governance gov = new Governance(
                    "pci-dss", platforms,
                    BranchingModel.TRUNK, false);

            assertThat(gov.compliance()).isEqualTo("pci-dss");
            assertThat(gov.platforms())
                    .containsExactly(Platform.CLAUDE_CODE);
            assertThat(gov.branchingModel())
                    .isEqualTo(BranchingModel.TRUNK);
            assertThat(gov.telemetryEnabled()).isFalse();
        }

        @Test
        @DisplayName("null platforms coerces to empty set")
        void ctor_nullPlatforms_emptySet() {
            Governance gov = new Governance(
                    "none", null,
                    BranchingModel.GITFLOW, true);

            assertThat(gov.platforms()).isEmpty();
        }

        @Test
        @DisplayName("null branchingModel defaults to GITFLOW")
        void ctor_nullBranchingModel_defaultsGitFlow() {
            Governance gov = new Governance(
                    "none", Set.of(), null, true);

            assertThat(gov.branchingModel())
                    .isEqualTo(BranchingModel.GITFLOW);
        }

        @Test
        @DisplayName("defensively copies the platforms set")
        void ctor_mutatingOriginalSet_doesNotAffectGovernance() {
            Set<Platform> mutable = new HashSet<>();
            mutable.add(Platform.CLAUDE_CODE);

            Governance gov = new Governance(
                    "none", mutable,
                    BranchingModel.GITFLOW, true);

            mutable.add(Platform.SHARED);

            assertThat(gov.platforms()).hasSize(1);
        }

        @Test
        @DisplayName("exposed platforms set is immutable")
        void ctor_returnedPlatforms_isUnmodifiable() {
            Governance gov = new Governance(
                    "none", Set.of(Platform.CLAUDE_CODE),
                    BranchingModel.GITFLOW, true);

            assertThatThrownBy(() ->
                    gov.platforms().add(Platform.SHARED))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("returns defaults when all meta fields absent")
        void fromMap_emptyRoot_allDefaults() {
            Governance gov = Governance.fromMap(Map.of());

            assertThat(gov.compliance()).isEqualTo("none");
            assertThat(gov.platforms()).isEmpty();
            assertThat(gov.branchingModel())
                    .isEqualTo(BranchingModel.GITFLOW);
            assertThat(gov.telemetryEnabled()).isTrue();
        }

        @Test
        @DisplayName("honours telemetry.enabled=false")
        void fromMap_telemetryDisabled_flagFalse() {
            Map<String, Object> root = Map.of(
                    "telemetry", Map.of("enabled", false));

            Governance gov = Governance.fromMap(root);

            assertThat(gov.telemetryEnabled()).isFalse();
        }

        @Test
        @DisplayName("throws on unsupported compliance value")
        void fromMap_unsupportedCompliance_throws() {
            Map<String, Object> root = Map.of(
                    "compliance", "hipaa");

            assertThatThrownBy(() -> Governance.fromMap(root))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining("compliance");
        }

        @Test
        @DisplayName("throws on invalid branching-model value")
        void fromMap_invalidBranchingModel_throws() {
            Map<String, Object> root = Map.of(
                    "branching-model", "octopus");

            assertThatThrownBy(() -> Governance.fromMap(root))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining("branching-model");
        }
    }
}
