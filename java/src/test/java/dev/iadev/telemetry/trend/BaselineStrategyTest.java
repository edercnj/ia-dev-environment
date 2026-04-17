package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BaselineStrategyTest {

    @Test
    void parse_mean_returnsMean() {
        assertThat(BaselineStrategy.parse("mean"))
                .isEqualTo(BaselineStrategy.MEAN);
    }

    @Test
    void parse_median_returnsMedian() {
        assertThat(BaselineStrategy.parse("median"))
                .isEqualTo(BaselineStrategy.MEDIAN);
    }

    @Test
    void parse_caseInsensitive_returnsMean() {
        assertThat(BaselineStrategy.parse("MEAN"))
                .isEqualTo(BaselineStrategy.MEAN);
    }

    @Test
    void parse_null_throwsIllegalArgument() {
        assertThatThrownBy(() -> BaselineStrategy.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_unknownValue_throwsIllegalArgument() {
        assertThatThrownBy(() -> BaselineStrategy.parse("mode"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mean")
                .hasMessageContaining("median");
    }
}
