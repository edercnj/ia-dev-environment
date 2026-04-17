package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrendJsonRendererTest {

    private final TrendJsonRenderer renderer =
            new TrendJsonRenderer();

    @Test
    void render_emptyReport_producesValidJson() {
        TrendReport report = new TrendReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0001", "EPIC-0002"),
                20.0,
                "MEAN",
                List.of(),
                List.of());
        String json = renderer.render(report);
        assertThat(json)
                .startsWith("{")
                .contains("\"generatedAt\"")
                .contains("\"epicsAnalyzed\"")
                .contains("\"thresholdPct\"")
                .contains("\"baseline\"")
                .contains("\"regressions\"")
                .contains("\"slowest\"");
    }

    @Test
    void render_withPayload_includesFields() {
        TrendReport report = new TrendReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0001", "EPIC-0002"),
                20.0,
                "MEDIAN",
                List.of(new Regression("foo", 100L, 140L, 40.0,
                        List.of("EPIC-0001", "EPIC-0002"))),
                List.of(new SlowSkill("foo", 140L, 10L)));
        String json = renderer.render(report);
        assertThat(json)
                .contains("\"skill\" : \"foo\"")
                .contains("\"baselineP95Ms\" : 100")
                .contains("\"currentP95Ms\" : 140")
                .contains("\"deltaPct\" : 40.0")
                .contains("\"avgP95Ms\" : 140");
    }
}
