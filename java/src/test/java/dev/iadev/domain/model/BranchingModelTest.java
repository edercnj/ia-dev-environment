package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BranchingModel")
class BranchingModelTest {

    @Nested
    @DisplayName("enum values")
    class EnumValues {

        @Test
        @DisplayName("GITFLOW has configValue 'gitflow'")
        void gitflow_configValue_returnsGitflow() {
            assertThat(BranchingModel.GITFLOW.configValue())
                    .isEqualTo("gitflow");
        }

        @Test
        @DisplayName("TRUNK has configValue 'trunk'")
        void trunk_configValue_returnsTrunk() {
            assertThat(BranchingModel.TRUNK.configValue())
                    .isEqualTo("trunk");
        }

        @Test
        @DisplayName("GITFLOW baseBranch is 'develop'")
        void gitflow_baseBranch_returnsDevelop() {
            assertThat(BranchingModel.GITFLOW.baseBranch())
                    .isEqualTo("develop");
        }

        @Test
        @DisplayName("TRUNK baseBranch is 'main'")
        void trunk_baseBranch_returnsMain() {
            assertThat(BranchingModel.TRUNK.baseBranch())
                    .isEqualTo("main");
        }

        @Test
        @DisplayName("exactly two enum values exist")
        void values_count_isTwo() {
            assertThat(BranchingModel.values())
                    .hasSize(2);
        }
    }

    @Nested
    @DisplayName("fromConfigValue()")
    class FromConfigValue {

        @Test
        @DisplayName("'gitflow' resolves to GITFLOW")
        void fromConfigValue_gitflow_returnsGitflow() {
            Optional<BranchingModel> result =
                    BranchingModel.fromConfigValue("gitflow");

            assertThat(result)
                    .contains(BranchingModel.GITFLOW);
        }

        @Test
        @DisplayName("'trunk' resolves to TRUNK")
        void fromConfigValue_trunk_returnsTrunk() {
            Optional<BranchingModel> result =
                    BranchingModel.fromConfigValue("trunk");

            assertThat(result)
                    .contains(BranchingModel.TRUNK);
        }

        @Test
        @DisplayName("'GITFLOW' (uppercase) resolves")
        void fromConfigValue_uppercase_resolves() {
            Optional<BranchingModel> result =
                    BranchingModel
                            .fromConfigValue("GITFLOW");

            assertThat(result)
                    .contains(BranchingModel.GITFLOW);
        }

        @Test
        @DisplayName("'Trunk' (mixed case) resolves")
        void fromConfigValue_mixedCase_resolves() {
            Optional<BranchingModel> result =
                    BranchingModel
                            .fromConfigValue("Trunk");

            assertThat(result)
                    .contains(BranchingModel.TRUNK);
        }

        @Test
        @DisplayName("' gitflow ' (whitespace) resolves")
        void fromConfigValue_whitespace_resolves() {
            Optional<BranchingModel> result =
                    BranchingModel
                            .fromConfigValue(" gitflow ");

            assertThat(result)
                    .contains(BranchingModel.GITFLOW);
        }

        @Test
        @DisplayName("null returns empty")
        void fromConfigValue_null_returnsEmpty() {
            Optional<BranchingModel> result =
                    BranchingModel.fromConfigValue(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("empty string returns empty")
        void fromConfigValue_empty_returnsEmpty() {
            Optional<BranchingModel> result =
                    BranchingModel.fromConfigValue("");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("invalid value returns empty")
        void fromConfigValue_invalid_returnsEmpty() {
            Optional<BranchingModel> result =
                    BranchingModel
                            .fromConfigValue("invalid");

            assertThat(result).isEmpty();
        }
    }
}
