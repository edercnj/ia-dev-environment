package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Stability test: the migrated exemplar at
 * {@code plans/epic-0038/examples/task-TASK-0037-0001-001.md} must remain byte-for-byte
 * identical to its reference copy at
 * {@code java/src/test/resources/smoke/epic-0038/examples/task-TASK-0037-0001-001.md}.
 *
 * <p>Any intentional change to the exemplar MUST be accompanied by a matching update to
 * the reference file, keeping authors honest about schema-fixture drift.</p>
 *
 * <p>The reference lives under {@code smoke/} (not {@code golden/}) because
 * {@code golden/} is reserved for per-profile pipeline output and is validated by
 * {@code GoldenFileCoverageTest} against {@code SmokeProfiles.profileList()} — a
 * non-profile fixture there would be flagged as an orphan.</p>
 */
class TaskExampleGoldenTest {

    private static final Path SOURCE = Path.of(
            "..", "plans", "epic-0038", "examples", "task-TASK-0037-0001-001.md");

    private static final Path GOLDEN = Path.of(
            "src", "test", "resources", "smoke", "epic-0038",
            "examples", "task-TASK-0037-0001-001.md");

    @Test
    void exampleFile_isByteForByteIdenticalToGolden() throws IOException {
        byte[] source = Files.readAllBytes(SOURCE);
        byte[] golden = Files.readAllBytes(GOLDEN);
        assertThat(source)
                .as("exemplar drifted from golden; update golden or revert the exemplar change")
                .containsExactly(golden);
    }

    @Test
    void goldenFile_existsAndIsNonEmpty() throws IOException {
        assertThat(Files.exists(GOLDEN))
                .as("golden file missing at %s", GOLDEN)
                .isTrue();
        assertThat(Files.size(GOLDEN)).isPositive();
    }
}
