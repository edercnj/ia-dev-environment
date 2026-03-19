package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParsedMapTest {

    @Nested
    class Construction {

        @Test
        void create_allFieldsAccessible() {
            var stories = new LinkedHashMap<String, DagNode>();
            stories.put("s-001", new DagNode("s-001", "Root",
                    new ArrayList<>(), new ArrayList<>()));

            var phases = Map.of(0, List.of("s-001"));
            var criticalPath = List.of("s-001");
            var warnings = List.of(
                    new DagWarning(
                            DagWarning.Type.ASYMMETRIC_DEPENDENCY,
                            "test warning"));

            var parsed = new ParsedMap(
                    stories, phases, criticalPath,
                    1, warnings);

            assertThat(parsed.stories()).hasSize(1);
            assertThat(parsed.phases()).hasSize(1);
            assertThat(parsed.criticalPath())
                    .containsExactly("s-001");
            assertThat(parsed.totalPhases()).isEqualTo(1);
            assertThat(parsed.warnings()).hasSize(1);
        }

        @Test
        void create_defensiveCopy_storiesImmutable() {
            var stories = new LinkedHashMap<String, DagNode>();
            stories.put("s-001", new DagNode("s-001", "Root",
                    new ArrayList<>(), new ArrayList<>()));

            var parsed = new ParsedMap(
                    stories, Map.of(0, List.of("s-001")),
                    List.of("s-001"), 1, List.of());

            assertThatThrownBy(
                    () -> parsed.stories().put("s-002", null))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }

        @Test
        void create_defensiveCopy_criticalPathImmutable() {
            var parsed = new ParsedMap(
                    Map.of(), Map.of(),
                    new ArrayList<>(List.of("s-001")),
                    0, List.of());

            assertThatThrownBy(
                    () -> parsed.criticalPath().add("s-002"))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }
    }
}
