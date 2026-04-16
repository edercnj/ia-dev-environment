package dev.iadev.release.preflight;

import dev.iadev.release.BumpType;
import dev.iadev.release.CommitCounts;
import dev.iadev.release.ReleaseContext;
import dev.iadev.release.SemVer;
import dev.iadev.release.integrity.IntegrityReport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the hotfix-aware
 * {@link PreflightDashboardRenderer} render overload
 * (story-0039-0014 TASK-009).
 */
@DisplayName("PreflightDashboardRenderer — hotfix")
class PreflightDashboardRendererHotfixTest {

    private static DashboardData sampleData() {
        return new DashboardData(
                new SemVer(3, 1, 1, null),
                Optional.of(new SemVer(3, 1, 0, null)),
                5L,
                new CommitCounts(0, 2, 0, 0, 1),
                BumpType.PATCH,
                List.of("- fix: bump"),
                IntegrityReport.aggregate(List.of()),
                "main");
    }

    @Test
    @DisplayName("renders modo HOTFIX banner when "
            + "ctx=hotfix")
    void render_hotfixBanner() {
        String rendered = PreflightDashboardRenderer.render(
                sampleData(), 10,
                ReleaseContext.forHotfix());

        assertThat(rendered)
                .contains("modo HOTFIX")
                .contains("base=main")
                .contains("bump=PATCH");
    }

    @Test
    @DisplayName("omits HOTFIX banner for standard flow")
    void render_standardHasNoBanner() {
        String rendered = PreflightDashboardRenderer.render(
                sampleData(), 10,
                ReleaseContext.release());

        assertThat(rendered).doesNotContain("modo HOTFIX");
    }

    @Test
    @DisplayName("header says hotfix vX.Y.Z")
    void render_hotfixHeader() {
        String rendered = PreflightDashboardRenderer.render(
                sampleData(), 10,
                ReleaseContext.forHotfix());

        assertThat(rendered).contains("hotfix v3.1.1");
    }
}
