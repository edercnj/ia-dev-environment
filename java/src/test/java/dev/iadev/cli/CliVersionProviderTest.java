package dev.iadev.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CliVersionProvider}.
 *
 * <p>The version is sourced from the filtered Maven resource
 * {@code dev/iadev/version.properties}, so these tests rely on
 * the resource being on the test classpath via Maven's standard
 * resource processing.
 */
class CliVersionProviderTest {

    @Test
    void getVersion_whenCalled_returnsSingleLine() throws Exception {
        String[] result = new CliVersionProvider().getVersion();

        assertThat(result).hasSize(1);
    }

    @Test
    void getVersion_whenCalled_startsWithProgramName() throws Exception {
        String[] result = new CliVersionProvider().getVersion();

        assertThat(result[0]).startsWith("ia-dev-env ");
    }

    @Test
    void getVersion_whenCalled_containsSemVerNumber() throws Exception {
        String[] result = new CliVersionProvider().getVersion();

        assertThat(result[0])
                .matches("ia-dev-env \\d+\\.\\d+\\.\\d+.*");
    }

    @Test
    void getVersion_whenCalled_doesNotReturnUnknownPlaceholder()
            throws Exception {
        String[] result = new CliVersionProvider().getVersion();

        assertThat(result[0]).doesNotContain(
                CliVersionProvider.UNKNOWN_VERSION);
    }

    @Test
    void getVersion_whenCalled_doesNotLeakUnfilteredPlaceholder()
            throws Exception {
        String[] result = new CliVersionProvider().getVersion();

        assertThat(result[0]).doesNotContain("${project.version}");
    }
}
