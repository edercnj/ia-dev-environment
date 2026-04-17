package dev.iadev.parallelism;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("performance")
class ParallelEvalPerformanceTest {

    @TempDir
    Path tmp;

    @Test
    void analyze15SyntheticStories_under3Seconds()
            throws IOException {
        for (int i = 1; i <= 15; i++) {
            String id = String.format(
                    "story-0099-%04d", i);
            String body = """
                    ## 1. Dependências
                    | Blocked By | Blocks |
                    | :--- | :--- |
                    | — | — |
                    ## File Footprint
                    ### write:
                    - java/src/main/java/pkg/Gen%d.java
                    - java/src/main/java/pkg/shared/Util.java
                    """.formatted(i);
            Files.writeString(
                    tmp.resolve(id + ".md"), body);
        }
        ParallelismEvaluator evaluator =
                new ParallelismEvaluator();
        long t0 = System.nanoTime();
        var report = evaluator.evaluateEpic(tmp);
        long elapsedMs =
                (System.nanoTime() - t0) / 1_000_000;
        assertThat(elapsedMs)
                .as("analysis of 15 stories should be "
                        + "below 3000 ms (was "
                        + elapsedMs + " ms)")
                .isLessThan(3000);
        assertThat(report.itemsAnalyzed()).isEqualTo(15);
    }
}
