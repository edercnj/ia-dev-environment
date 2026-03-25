package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DagNodeTest {

    @Nested
    class Construction {

        @Test
        void create_defaultPhase_isMinusOne() {
            var node = new DagNode(
                    "s-001", "Root", Optional.empty(),
                    new ArrayList<>(), new ArrayList<>());

            assertThat(node.phase()).isEqualTo(-1);
        }

        @Test
        void create_defaultCriticalPath_isFalse() {
            var node = new DagNode(
                    "s-001", "Root", Optional.empty(),
                    new ArrayList<>(), new ArrayList<>());

            assertThat(node.isOnCriticalPath()).isFalse();
        }

        @Test
        void create_accessors_returnCorrectValues() {
            var blocked = new ArrayList<String>();
            blocked.add("s-002");
            var blocks = new ArrayList<String>();
            blocks.add("s-003");

            var node = new DagNode(
                    "s-001", "Title", Optional.empty(),
                    blocked, blocks);

            assertThat(node.storyId()).isEqualTo("s-001");
            assertThat(node.title()).isEqualTo("Title");
            assertThat(node.blockedBy())
                    .containsExactly("s-002");
            assertThat(node.blocks())
                    .containsExactly("s-003");
        }

        @Test
        void create_withJiraKey_keyAccessible() {
            var node = new DagNode(
                    "s-001", "Root",
                    Optional.of("PROJ-42"),
                    new ArrayList<>(), new ArrayList<>());

            assertThat(node.jiraKey())
                    .isPresent()
                    .hasValue("PROJ-42");
        }

        @Test
        void create_withEmptyJiraKey_keyIsEmpty() {
            var node = new DagNode(
                    "s-001", "Root", Optional.empty(),
                    new ArrayList<>(), new ArrayList<>());

            assertThat(node.jiraKey()).isEmpty();
        }

        @Test
        void create_withNullJiraKey_defaultsToEmpty() {
            var node = new DagNode(
                    "s-001", "Root", null,
                    new ArrayList<>(), new ArrayList<>());

            assertThat(node.jiraKey()).isEmpty();
        }
    }

    @Nested
    class Mutation {

        @Test
        void setPhase_whenCalled_updatesPhase() {
            var node = new DagNode(
                    "s-001", "Root", Optional.empty(),
                    new ArrayList<>(), new ArrayList<>());

            node.setPhase(2);

            assertThat(node.phase()).isEqualTo(2);
        }

        @Test
        void setOnCriticalPath_whenCalled_updatesFlag() {
            var node = new DagNode(
                    "s-001", "Root", Optional.empty(),
                    new ArrayList<>(), new ArrayList<>());

            node.setOnCriticalPath(true);

            assertThat(node.isOnCriticalPath()).isTrue();
        }
    }

    @Test
    void toString_whenCalled_containsStoryIdAndPhase() {
        var node = new DagNode(
                "s-001", "Root", Optional.empty(),
                new ArrayList<>(), new ArrayList<>());
        node.setPhase(0);

        assertThat(node.toString())
                .contains("s-001")
                .contains("phase=0");
    }
}
