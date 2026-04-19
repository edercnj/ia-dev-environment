package dev.iadev.checkpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParallelismDowngradeBranchTest {

    private static final Instant T = Instant.parse(
            "2026-04-17T12:00:00Z");

    @Test
    @DisplayName("nullCollections_areNormalizedToEmpty")
    void nullCollections_normalizedToEmpty() {
        ParallelismDowngrade d = new ParallelismDowngrade(
                1, null, null, "r", T);
        assertThat(d.originalGroup()).isEmpty();
        assertThat(d.adjustedSequence()).isEmpty();
    }

    @Test
    @DisplayName("nullInnerListInsideAdjustedSequence_isNormalizedToEmpty")
    void nullInnerList_normalizedToEmpty() {
        List<List<String>> seq = new ArrayList<>();
        seq.add(null);
        seq.add(Arrays.asList("a"));
        ParallelismDowngrade d = new ParallelismDowngrade(
                2, List.of("g1"), seq, "r", T);
        assertThat(d.adjustedSequence()).hasSize(2);
        assertThat(d.adjustedSequence().get(0)).isEmpty();
        assertThat(d.adjustedSequence().get(1))
                .containsExactly("a");
    }

    @Test
    @DisplayName("equals_coversAllBranches")
    void equals_coversAllBranches() {
        ParallelismDowngrade base = new ParallelismDowngrade(
                1, List.of("g"), List.of(List.of("a")),
                "r", T);

        // self
        assertThat(base).isEqualTo(base);
        // null
        assertThat(base.equals(null)).isFalse();
        // wrong type
        assertThat(base.equals("string")).isFalse();
        // equal twin
        ParallelismDowngrade twin = new ParallelismDowngrade(
                1, List.of("g"), List.of(List.of("a")),
                "r", T);
        assertThat(base).isEqualTo(twin);
        assertThat(base).hasSameHashCodeAs(twin);
        // different phase
        assertThat(base).isNotEqualTo(new ParallelismDowngrade(
                2, List.of("g"), List.of(List.of("a")),
                "r", T));
        // different reason
        assertThat(base).isNotEqualTo(new ParallelismDowngrade(
                1, List.of("g"), List.of(List.of("a")),
                "other", T));
        // different timestamp
        assertThat(base).isNotEqualTo(new ParallelismDowngrade(
                1, List.of("g"), List.of(List.of("a")),
                "r", Instant.EPOCH));
        // different originalGroup
        assertThat(base).isNotEqualTo(new ParallelismDowngrade(
                1, List.of("other"), List.of(List.of("a")),
                "r", T));
        // different adjustedSequence
        assertThat(base).isNotEqualTo(new ParallelismDowngrade(
                1, List.of("g"), List.of(List.of("z")),
                "r", T));
    }
}
