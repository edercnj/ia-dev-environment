package dev.iadev.domain.stack;

import dev.iadev.domain.model.BranchingModel;
import dev.iadev.testutil.TestConfigBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for StackValidator — branching model validation.
 */
@DisplayName("StackValidator — branching model")
class StackValidatorBranchingModelTest {

    @Test
    @DisplayName("GITFLOW passes validation")
    void validateBranchingModel_gitflow_noErrors() {
        var config = TestConfigBuilder.builder()
                .branchingModel(BranchingModel.GITFLOW)
                .build();

        var errors = StackValidator
                .validateBranchingModel(config);

        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("TRUNK passes validation")
    void validateBranchingModel_trunk_noErrors() {
        var config = TestConfigBuilder.builder()
                .branchingModel(BranchingModel.TRUNK)
                .build();

        var errors = StackValidator
                .validateBranchingModel(config);

        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("default (null) resolves to GITFLOW")
    void validateBranchingModel_default_noErrors() {
        var config = TestConfigBuilder.builder()
                .build();

        var errors = StackValidator
                .validateBranchingModel(config);

        assertThat(errors).isEmpty();
        assertThat(config.branchingModel())
                .isEqualTo(BranchingModel.GITFLOW);
    }

    @Test
    @DisplayName("validateStack includes branching model")
    void validateStack_withBranchingModel_included() {
        var config = TestConfigBuilder.builder()
                .branchingModel(BranchingModel.TRUNK)
                .build();

        var errors = StackValidator
                .validateStack(config);

        assertThat(errors).isEmpty();
    }
}
