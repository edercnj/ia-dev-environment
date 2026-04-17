package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrendMarkdownRendererTest {

    private final TrendMarkdownRenderer renderer =
            new TrendMarkdownRenderer();

    @Test
    void render_emptyRegressions_showsPlaceholder() {
        TrendReport report = new TrendReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0001", "EPIC-0002"),
                20.0,
                "MEAN",
                List.of(),
                List.of());
        String md = renderer.render(report);
        assertThat(md)
                .contains("# Telemetry Trend Report")
                .contains("**Epics analyzed:** EPIC-0001, EPIC-0002")
                .contains("**Threshold:** 20.0%")
                .contains("**Baseline:** mean")
                .contains("## Top-10 regressions")
                .contains("No regressions detected")
                .contains("## Top-10 slowest skills")
                .contains("## Observations");
    }

    @Test
    void render_withRegressions_listsThem() {
        TrendReport report = new TrendReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0001", "EPIC-0002"),
                20.0,
                "MEAN",
                List.of(new Regression("foo", 100L, 140L, 40.0,
                        List.of("EPIC-0001", "EPIC-0002"))),
                List.of(new SlowSkill("foo", 140L, 10L)));
        String md = renderer.render(report);
        assertThat(md)
                .contains("| foo | 100 | 140 | 40.0 |")
                .contains("| foo | 140 | 10 |")
                .contains("1 skill(s) regressed >= 20.0%");
    }

    @Test
    void render_slowestOnly_noRegressionNote() {
        TrendReport report = new TrendReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0001", "EPIC-0002"),
                20.0,
                "MEDIAN",
                List.of(),
                List.of(new SlowSkill("x-story-implement",
                        12000L, 50L)));
        String md = renderer.render(report);
        assertThat(md)
                .contains("x-story-implement")
                .contains("Slowest skill (avg P95): "
                        + "x-story-implement");
    }
}
