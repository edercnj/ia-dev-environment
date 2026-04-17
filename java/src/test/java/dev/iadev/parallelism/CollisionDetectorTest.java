package dev.iadev.parallelism;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CollisionDetectorTest {

    private final CollisionDetector detector =
            new CollisionDetector();

    @Test
    void detect_disjointFootprints_returnsEmpty() {
        FileFootprint a = FileFootprint.ofWrites(
                Set.of("a.java"));
        FileFootprint b = FileFootprint.ofWrites(
                Set.of("b.java"));
        assertThat(detector.detect(
                "task-A", a, "task-B", b, CollisionPolicy.EXCLUDE_SOFT))
                .isEmpty();
    }

    @Test
    void detect_writeWrite_isHard() {
        FileFootprint a = FileFootprint.ofWrites(
                Set.of("shared.java", "a.java"));
        FileFootprint b = FileFootprint.ofWrites(
                Set.of("shared.java", "b.java"));
        Optional<Collision> out = detector.detect(
                "task-A", a, "task-B", b,
                CollisionPolicy.EXCLUDE_SOFT);
        assertThat(out).isPresent();
        Collision c = out.get();
        assertThat(c.category())
                .isEqualTo(CollisionCategory.HARD);
        assertThat(c.sharedPaths())
                .containsExactly("shared.java");
        assertThat(c.reason()).isNull();
    }

    @Test
    void detect_writeRegen_isRegen() {
        FileFootprint a = new FileFootprint(
                Set.of("java/src/main/resources/"
                        + "targets/claude/rules/21.md"),
                Set.of(),
                Set.of());
        FileFootprint b = new FileFootprint(
                Set.of("something-else.java"),
                Set.of(),
                Set.of("java/src/main/resources/"
                        + "targets/claude/rules/21.md"));
        Optional<Collision> out = detector.detect(
                "A", a, "B", b, CollisionPolicy.EXCLUDE_SOFT);
        assertThat(out).isPresent();
        assertThat(out.get().category())
                .isEqualTo(CollisionCategory.REGEN);
    }

    @Test
    void detect_regenRegen_isRegen() {
        FileFootprint a = new FileFootprint(
                Set.of("w1.java"),
                Set.of(),
                Set.of(".claude/skills/x/SKILL.md"));
        FileFootprint b = new FileFootprint(
                Set.of("w2.java"),
                Set.of(),
                Set.of(".claude/skills/x/SKILL.md"));
        assertThat(detector.detect(
                "A", a, "B", b,
                CollisionPolicy.EXCLUDE_SOFT)
                .get().category())
                .isEqualTo(CollisionCategory.REGEN);
    }

    @Test
    void detect_readRead_withSoftIncluded_isSoft() {
        FileFootprint a = new FileFootprint(
                Set.of("w1.java"),
                Set.of("common.md"),
                Set.of());
        FileFootprint b = new FileFootprint(
                Set.of("w2.java"),
                Set.of("common.md"),
                Set.of());
        Optional<Collision> out = detector.detect(
                "A", a, "B", b, CollisionPolicy.INCLUDE_SOFT);
        assertThat(out).isPresent();
        assertThat(out.get().category())
                .isEqualTo(CollisionCategory.SOFT);
    }

    @Test
    void detect_readRead_withSoftFiltered_isEmpty() {
        FileFootprint a = new FileFootprint(
                Set.of("w1.java"),
                Set.of("common.md"),
                Set.of());
        FileFootprint b = new FileFootprint(
                Set.of("w2.java"),
                Set.of("common.md"),
                Set.of());
        assertThat(detector.detect(
                "A", a, "B", b, CollisionPolicy.EXCLUDE_SOFT)).isEmpty();
    }

    @Test
    void detect_hotspotTouchedByBoth_overridesToHard() {
        HotspotCatalog cat = new HotspotCatalog(
                List.of("pom.xml"));
        CollisionDetector d = new CollisionDetector(cat);
        FileFootprint a = FileFootprint.ofWrites(
                Set.of("pom.xml"));
        FileFootprint b = FileFootprint.ofWrites(
                Set.of("pom.xml"));
        Collision c = d.detect("A", a, "B", b,
                CollisionPolicy.EXCLUDE_SOFT)
                .orElseThrow();
        assertThat(c.category())
                .isEqualTo(CollisionCategory.HARD);
        assertThat(c.reason()).startsWith("hotspot: ");
    }

    @Test
    void detect_hotspotViaDistinctPaths_matchingGlob_isHard() {
        HotspotCatalog cat = new HotspotCatalog(
                List.of("java/src/test/resources/"
                        + "golden/**"));
        CollisionDetector d = new CollisionDetector(cat);
        FileFootprint a = new FileFootprint(
                Set.of(),
                Set.of(),
                Set.of("java/src/test/resources/"
                        + "golden/a/file.yml"));
        FileFootprint b = new FileFootprint(
                Set.of(),
                Set.of(),
                Set.of("java/src/test/resources/"
                        + "golden/b/file.yml"));
        Collision c = d.detect("A", a, "B", b,
                CollisionPolicy.EXCLUDE_SOFT)
                .orElseThrow();
        assertThat(c.category())
                .isEqualTo(CollisionCategory.HARD);
        assertThat(c.reason())
                .contains("java/src/test/resources/golden");
    }

    @Test
    void detect_sameId_returnsEmpty() {
        FileFootprint fp = FileFootprint.ofWrites(
                Set.of("a.java"));
        assertThat(detector.detect(
                "A", fp, "A", fp, CollisionPolicy.EXCLUDE_SOFT)).isEmpty();
    }

    @Test
    void detect_swapArguments_sameOutput() {
        FileFootprint a = FileFootprint.ofWrites(
                Set.of("shared.java"));
        FileFootprint b = FileFootprint.ofWrites(
                Set.of("shared.java"));
        Collision ab = detector.detect(
                "taskA", a, "taskB", b,
                CollisionPolicy.EXCLUDE_SOFT)
                .orElseThrow();
        Collision ba = detector.detect(
                "taskB", b, "taskA", a,
                CollisionPolicy.EXCLUDE_SOFT)
                .orElseThrow();
        assertThat(ab).isEqualTo(ba);
    }
}
