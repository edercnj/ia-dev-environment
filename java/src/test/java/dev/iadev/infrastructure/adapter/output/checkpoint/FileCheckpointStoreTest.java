package dev.iadev.infrastructure.adapter.output.checkpoint;

import dev.iadev.domain.model.CheckpointState;
import dev.iadev.domain.port.output.CheckpointStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link FileCheckpointStore}.
 *
 * <p>Covers all Gherkin acceptance criteria (GK-1 through GK-5)
 * and additional edge cases for robustness.</p>
 */
class FileCheckpointStoreTest {

    @TempDir
    Path tempDir;

    private FileCheckpointStore store;

    @BeforeEach
    void setUp() {
        store = new FileCheckpointStore(tempDir);
    }

    @Nested
    class ImplementsPort {

        @Test
        void create_implementsCheckpointStoreInterface() {
            assertThat(store)
                    .isInstanceOf(CheckpointStore.class);
        }
    }

    @Nested
    class LoadNonExistent {

        /** @GK-1: Load of non-existent checkpoint. */
        @Test
        void load_nonExistentId_returnsEmpty() {
            var result = store.load("nonexistent-id");

            assertThat(result).isEmpty();
        }

        @Test
        void load_nonExistentId_doesNotThrow() {
            var result = store.load("nonexistent-id");

            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SaveAndLoad {

        /** @GK-2: Save and load round-trip. */
        @Test
        void saveAndLoad_roundTrip_preservesState() {
            var state = createSampleState("exec-001");

            store.save(state);
            var loaded = store.load("exec-001");

            assertThat(loaded).isPresent();
            assertThat(loaded.get().executionId())
                    .isEqualTo("exec-001");
            assertThat(loaded.get().createdAt())
                    .isEqualTo(state.createdAt());
            assertThat(loaded.get().updatedAt())
                    .isEqualTo(state.updatedAt());
            assertThat(loaded.get().completedSteps())
                    .isEqualTo(state.completedSteps());
        }

        @Test
        void save_createsJsonFile() {
            var state = createSampleState("exec-001");

            store.save(state);

            Path expectedFile =
                    tempDir.resolve("exec-001.json");
            assertThat(expectedFile).exists();
        }

        @Test
        void save_writesValidJson() throws IOException {
            var state = createSampleState("exec-001");

            store.save(state);

            Path file = tempDir.resolve("exec-001.json");
            String json = Files.readString(file);
            assertThat(json).contains("\"executionId\"");
            assertThat(json).contains("exec-001");
        }

        @Test
        void save_overwritesExisting() {
            var state1 = createSampleState("exec-001");
            store.save(state1);

            var state2 = new CheckpointState(
                    "exec-001",
                    Instant.parse("2026-01-01T00:00:00Z"),
                    Instant.parse("2026-01-02T00:00:00Z"),
                    Map.of("step-a", "done",
                            "step-b", "done"),
                    Map.of());
            store.save(state2);

            var loaded = store.load("exec-001");
            assertThat(loaded).isPresent();
            assertThat(loaded.get().completedSteps())
                    .hasSize(2);
        }
    }

    @Nested
    class Clear {

        /** @GK-3: Clear removes existing checkpoint. */
        @Test
        void clear_existingCheckpoint_removesFile() {
            var state = createSampleState("exec-001");
            store.save(state);

            store.clear("exec-001");

            Path file = tempDir.resolve("exec-001.json");
            assertThat(file).doesNotExist();
        }

        @Test
        void clear_existingCheckpoint_loadReturnsEmpty() {
            var state = createSampleState("exec-001");
            store.save(state);

            store.clear("exec-001");

            assertThat(store.load("exec-001")).isEmpty();
        }

        /** @GK-4: Clear of non-existent checkpoint. */
        @Test
        void clear_nonExistent_doesNotThrow() {
            store.clear("nonexistent");

            // No exception means success
            assertThat(tempDir).isDirectory();
        }
    }

    @Nested
    class LargeState {

        /** @GK-5: Large checkpoint state serialized. */
        @Test
        void saveAndLoad_largeState_preservesAllEntries() {
            Map<String, String> largeSteps =
                    IntStream.rangeClosed(1, 500)
                            .boxed()
                            .collect(Collectors.toMap(
                                    i -> "step-" + i,
                                    i -> "completed-" + i,
                                    (a, b) -> a,
                                    LinkedHashMap::new));

            var state = new CheckpointState(
                    "large-exec",
                    Instant.parse("2026-01-01T00:00:00Z"),
                    Instant.parse("2026-01-01T12:00:00Z"),
                    largeSteps,
                    Map.of("batchSize", 500));

            store.save(state);
            var loaded = store.load("large-exec");

            assertThat(loaded).isPresent();
            assertThat(loaded.get().completedSteps())
                    .hasSize(500);
            assertThat(loaded.get().completedSteps()
                    .get("step-1"))
                    .isEqualTo("completed-1");
            assertThat(loaded.get().completedSteps()
                    .get("step-500"))
                    .isEqualTo("completed-500");
        }
    }

    @Nested
    class Validation {

        @Test
        void save_nullState_throwsIllegalArgument() {
            assertThatThrownBy(() -> store.save(null))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        void load_nullExecutionId_throwsIllegalArgument() {
            assertThatThrownBy(() -> store.load(null))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        void load_blankExecutionId_throwsIllegalArgument() {
            assertThatThrownBy(() -> store.load("  "))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        void clear_nullExecutionId_throwsIllegalArgument() {
            assertThatThrownBy(() -> store.clear(null))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        void clear_blankExecutionId_throwsIllegalArgument() {
            assertThatThrownBy(() -> store.clear("  "))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }
    }

    @Nested
    class AtomicWrite {

        @Test
        void save_noTmpFileRemainsAfterWrite() {
            var state = createSampleState("atomic-test");

            store.save(state);

            Path tmpFile =
                    tempDir.resolve(".atomic-test.json.tmp");
            assertThat(tmpFile).doesNotExist();
        }
    }

    @Nested
    class DirectoryCreation {

        @Test
        void save_createsDirectoryIfNotExists(
                @TempDir Path parentDir) {
            Path subDir = parentDir.resolve("nested/deep");
            var nestedStore =
                    new FileCheckpointStore(subDir);
            var state = createSampleState("dir-test");

            nestedStore.save(state);

            assertThat(subDir.resolve("dir-test.json"))
                    .exists();
        }
    }

    private static CheckpointState createSampleState(
            String executionId) {
        return new CheckpointState(
                executionId,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T12:00:00Z"),
                Map.of("step-1", "completed"),
                Map.of("profile", "java-spring"));
    }
}
