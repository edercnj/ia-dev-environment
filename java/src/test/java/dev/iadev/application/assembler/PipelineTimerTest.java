package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PipelineTimer} — elapsed-time
 * measurement helper decoupled from the pipeline
 * orchestrator.
 */
@DisplayName("PipelineTimer")
class PipelineTimerTest {

    @Test
    @DisplayName("time invokes the action exactly once")
    void time_invokesActionExactlyOnce() {
        AtomicInteger counter = new AtomicInteger();
        PipelineTimer timer = new PipelineTimer();

        timer.time(counter::incrementAndGet);

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("time returns a non-negative elapsed"
            + " duration in milliseconds")
    void time_returnsNonNegativeDuration() {
        PipelineTimer timer = new PipelineTimer();

        long durationMs = timer.time(() -> {
            // trivial work
        });

        assertThat(durationMs).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("time propagates exceptions from the"
            + " action unchanged")
    void time_propagatesActionException() {
        PipelineTimer timer = new PipelineTimer();
        RuntimeException boom =
                new RuntimeException("boom");

        assertThatThrownBy(() ->
                timer.time(() -> {
                    throw boom;
                }))
                .isSameAs(boom);
    }

    @Test
    @DisplayName("time rejects null action")
    void time_nullAction_throwsNpe() {
        PipelineTimer timer = new PipelineTimer();

        assertThatNullPointerException()
                .isThrownBy(() -> timer.time(null));
    }

    @Test
    @DisplayName("start/stop pair reports the elapsed time"
            + " since the captured marker")
    void startStop_reportsElapsedTime() throws Exception {
        PipelineTimer timer = new PipelineTimer();

        long start = timer.start();
        Thread.sleep(2);
        long elapsedMs = timer.stop(start);

        assertThat(elapsedMs).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("toMillis clamps negative durations to"
            + " zero")
    void toMillis_negativeDuration_clampedToZero() {
        assertThat(PipelineTimer.toMillis(-1L))
                .isEqualTo(0L);
    }

    @Test
    @DisplayName("toMillis converts nanoseconds to"
            + " milliseconds by integer division")
    void toMillis_convertsNanosToMillis() {
        assertThat(PipelineTimer.toMillis(5_000_000L))
                .isEqualTo(5L);
        assertThat(PipelineTimer.toMillis(999_999L))
                .isEqualTo(0L);
    }
}
