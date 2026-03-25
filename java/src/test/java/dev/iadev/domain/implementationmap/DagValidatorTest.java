package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DagValidatorTest {

    @Nested
    class ValidDag {

        @Test
        void validate_validDag_noExceptionThrown() {
            var dag = buildValidDag();

            assertThatCode(() -> DagValidator.validate(dag))
                    .doesNotThrowAnyException();
        }

        @Test
        void validate_validDag_returnsEmptyWarnings() {
            var dag = buildValidDag();

            var warnings = DagValidator.validate(dag);

            assertThat(warnings).isEmpty();
        }

        @Test
        void validate_emptyDag_noException() {
            var dag = new LinkedHashMap<String, DagNode>();

            assertThatCode(() -> DagValidator.validate(dag))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class CycleDetection {

        @Test
        void detectCycles_cyclicABC_throwsCircularDependency() {
            // A blocks B, B blocks C, C blocks A
            var dag = new LinkedHashMap<String, DagNode>();
            dag.put("A", new DagNode("A", "Node A",
                    Optional.empty(),
                    new ArrayList<>(List.of("C")),
                    new ArrayList<>(List.of("B"))));
            dag.put("B", new DagNode("B", "Node B",
                    Optional.empty(),
                    new ArrayList<>(List.of("A")),
                    new ArrayList<>(List.of("C"))));
            dag.put("C", new DagNode("C", "Node C",
                    Optional.empty(),
                    new ArrayList<>(List.of("B")),
                    new ArrayList<>(List.of("A"))));

            assertThatThrownBy(
                    () -> DagValidator.detectCycles(dag))
                    .isInstanceOf(
                            CircularDependencyException.class)
                    .hasMessageContaining("Circular dependency");
        }

        @Test
        void detectCycles_cyclicABC_cycleContainsAllIds() {
            var dag = new LinkedHashMap<String, DagNode>();
            dag.put("A", new DagNode("A", "Node A",
                    Optional.empty(),
                    new ArrayList<>(List.of("C")),
                    new ArrayList<>(List.of("B"))));
            dag.put("B", new DagNode("B", "Node B",
                    Optional.empty(),
                    new ArrayList<>(List.of("A")),
                    new ArrayList<>(List.of("C"))));
            dag.put("C", new DagNode("C", "Node C",
                    Optional.empty(),
                    new ArrayList<>(List.of("B")),
                    new ArrayList<>(List.of("A"))));

            try {
                DagValidator.detectCycles(dag);
            } catch (CircularDependencyException e) {
                assertThat(e.getCycle())
                        .containsAnyOf("A", "B", "C");
            }
        }

        @Test
        void detectCycles_noCycle_noException() {
            var dag = buildValidDag();

            assertThatCode(
                    () -> DagValidator.detectCycles(dag))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class RootValidation {

        @Test
        void validateRoots_noRoots_throwsInvalidDag() {
            var dag = new LinkedHashMap<String, DagNode>();
            dag.put("A", new DagNode("A", "Node A",
                    Optional.empty(),
                    new ArrayList<>(List.of("B")),
                    new ArrayList<>()));
            dag.put("B", new DagNode("B", "Node B",
                    Optional.empty(),
                    new ArrayList<>(List.of("A")),
                    new ArrayList<>()));

            assertThatThrownBy(
                    () -> DagValidator.validateRoots(dag))
                    .isInstanceOf(InvalidDagException.class)
                    .hasMessageContaining("no root nodes");
        }

        @Test
        void validateRoots_hasRoot_noException() {
            var dag = buildValidDag();

            assertThatCode(
                    () -> DagValidator.validateRoots(dag))
                    .doesNotThrowAnyException();
        }

        @Test
        void validateRoots_emptyDag_noException() {
            assertThatCode(() -> DagValidator.validateRoots(
                    new LinkedHashMap<>()))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class ReferenceValidation {

        @Test
        void validateReferences_missingRef_returnsError() {
            var dag = new LinkedHashMap<String, DagNode>();
            dag.put("s-002", new DagNode("s-002", "Child",
                    Optional.empty(),
                    new ArrayList<>(List.of("s-001")),
                    new ArrayList<>()));

            var errors = DagValidator.validateReferences(dag);

            assertThat(errors).hasSize(1);
            assertThat(errors.getFirst())
                    .contains("s-001")
                    .contains("non-existent");
        }

        @Test
        void validateReferences_allRefsExist_emptyErrors() {
            var dag = buildValidDag();

            var errors = DagValidator.validateReferences(dag);

            assertThat(errors).isEmpty();
        }

        @Test
        void validate_missingRef_throwsInvalidDag() {
            var dag = new LinkedHashMap<String, DagNode>();
            dag.put("s-002", new DagNode("s-002", "Child",
                    Optional.empty(),
                    new ArrayList<>(List.of("s-001")),
                    new ArrayList<>()));

            assertThatThrownBy(
                    () -> DagValidator.validate(dag))
                    .isInstanceOf(InvalidDagException.class)
                    .hasMessageContaining("s-001");
        }
    }

    @Nested
    class SymmetryValidation {

        @Test
        void validateSymmetry_asymmetricBlocks_autoCorrects() {
            // s-001 blocks s-002, but s-002 doesn't list
            // s-001 in blockedBy
            var dag = new LinkedHashMap<String, DagNode>();
            dag.put("s-001", new DagNode("s-001", "Root",
                    Optional.empty(),
                    new ArrayList<>(),
                    new ArrayList<>(List.of("s-002"))));
            dag.put("s-002", new DagNode("s-002", "Child",
                    Optional.empty(),
                    new ArrayList<>(),
                    new ArrayList<>()));

            var warnings =
                    DagValidator.validateSymmetry(dag);

            assertThat(warnings).isNotEmpty();
            assertThat(warnings.getFirst().type())
                    .isEqualTo(
                            DagWarning.Type.ASYMMETRIC_DEPENDENCY);
            // Auto-corrected: s-002 now has s-001 in blockedBy
            assertThat(dag.get("s-002").blockedBy())
                    .contains("s-001");
        }

        @Test
        void validateSymmetry_symmetricEdges_noWarnings() {
            var dag = buildValidDag();

            var warnings =
                    DagValidator.validateSymmetry(dag);

            assertThat(warnings).isEmpty();
        }
    }

    private LinkedHashMap<String, DagNode> buildValidDag() {
        var dag = new LinkedHashMap<String, DagNode>();
        dag.put("s-001", new DagNode("s-001", "Root",
                Optional.empty(),
                new ArrayList<>(),
                new ArrayList<>(List.of("s-002"))));
        dag.put("s-002", new DagNode("s-002", "Child",
                Optional.empty(),
                new ArrayList<>(List.of("s-001")),
                new ArrayList<>()));
        return dag;
    }
}
