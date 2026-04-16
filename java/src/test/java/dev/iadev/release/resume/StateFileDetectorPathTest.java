package dev.iadev.release.resume;

import dev.iadev.release.ReleaseContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for
 * {@link StateFileDetector#resolveStatePath(Path, String, ReleaseContext)}
 * (story-0039-0014 TASK-007/008/010).
 */
@DisplayName("StateFileDetector#resolveStatePath")
class StateFileDetectorPathTest {

    @Nested
    @DisplayName("standard release")
    class StandardRelease {

        @Test
        @DisplayName("produces release-state-X.Y.Z.json")
        void resolveStatePath_standard(
                @TempDir Path plansDir) {
            Path result = StateFileDetector.resolveStatePath(
                    plansDir, "3.2.0",
                    ReleaseContext.release());

            assertThat(result).isEqualTo(
                    plansDir.resolve(
                            "release-state-3.2.0.json"));
            assertThat(result.getFileName().toString())
                    .doesNotContain("hotfix");
        }
    }

    @Nested
    @DisplayName("hotfix release")
    class HotfixRelease {

        @Test
        @DisplayName("produces "
                + "release-state-hotfix-X.Y.Z.json")
        void resolveStatePath_hotfix(
                @TempDir Path plansDir) {
            Path result = StateFileDetector.resolveStatePath(
                    plansDir, "3.1.1",
                    ReleaseContext.forHotfix());

            assertThat(result).isEqualTo(
                    plansDir.resolve(
                            "release-state-hotfix-3.1.1"
                                    + ".json"));
        }

        @Test
        @DisplayName("does not collide with concurrent "
                + "release-normal state")
        void resolveStatePath_noCollision(
                @TempDir Path plansDir) {
            Path release =
                    StateFileDetector.resolveStatePath(
                            plansDir, "3.2.0",
                            ReleaseContext.release());
            Path hotfix =
                    StateFileDetector.resolveStatePath(
                            plansDir, "3.1.1",
                            ReleaseContext.forHotfix());

            assertThat(release).isNotEqualTo(hotfix);
            assertThat(release.getFileName())
                    .isNotEqualTo(hotfix.getFileName());
        }
    }

    @Nested
    @DisplayName("security — version validation")
    class SecurityValidation {

        @Test
        @DisplayName("rejects path traversal via "
                + "../ in version")
        void resolveStatePath_rejectsTraversal(
                @TempDir Path plansDir) {
            assertThatThrownBy(() ->
                    StateFileDetector.resolveStatePath(
                            plansDir, "../etc/passwd",
                            ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining(
                            "strict SemVer");
        }

        @Test
        @DisplayName("rejects empty string")
        void resolveStatePath_rejectsEmpty(
                @TempDir Path plansDir) {
            assertThatThrownBy(() ->
                    StateFileDetector.resolveStatePath(
                            plansDir, "",
                            ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects shell metacharacters")
        void resolveStatePath_rejectsShellMeta(
                @TempDir Path plansDir) {
            assertThatThrownBy(() ->
                    StateFileDetector.resolveStatePath(
                            plansDir, "3.1.1;rm -rf /",
                            ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects null version")
        void resolveStatePath_rejectsNull(
                @TempDir Path plansDir) {
            assertThatThrownBy(() ->
                    StateFileDetector.resolveStatePath(
                            plansDir, null,
                            ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            NullPointerException.class);
        }

        @Test
        @DisplayName("accepts SemVer with pre-release")
        void resolveStatePath_acceptsPreRelease(
                @TempDir Path plansDir) {
            Path result = StateFileDetector.resolveStatePath(
                    plansDir, "3.1.1-rc.1",
                    ReleaseContext.release());

            assertThat(result.getFileName().toString())
                    .isEqualTo(
                            "release-state-3.1.1-rc.1"
                                    + ".json");
        }
    }
}
