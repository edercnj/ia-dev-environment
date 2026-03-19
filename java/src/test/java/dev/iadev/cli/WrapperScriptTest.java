package dev.iadev.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@code bin/ia-dev-env} wrapper script.
 *
 * <p>Validates that the wrapper script exists, is executable,
 * has the correct structure with Java version detection,
 * and contains all required features.
 *
 * <p>Tests follow TPP ordering: existence, permissions,
 * content structure, Java detection logic.
 */
class WrapperScriptTest {

    private static final Path SCRIPT_PATH =
            Path.of("bin/ia-dev-env");

    @Test
    void wrapperScript_exists() {
        assertThat(SCRIPT_PATH).exists();
    }

    @Test
    void wrapperScript_isExecutable() {
        assertThat(SCRIPT_PATH.toFile().canExecute())
                .isTrue();
    }

    @Test
    void wrapperScript_startsWithShebang()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        assertThat(content)
                .startsWith("#!/usr/bin/env bash");
    }

    @Test
    void wrapperScript_containsJavaVersionCheck()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        assertThat(content)
                .contains("REQUIRED_JAVA_VERSION=21");
    }

    @Test
    void wrapperScript_containsJavaHomeDetection()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        assertThat(content).contains("JAVA_HOME");
    }

    @Test
    void wrapperScript_containsPathDetection()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        assertThat(content).contains("command -v java");
    }

    @Test
    void wrapperScript_supportsJavaOpts()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        assertThat(content)
                .contains("IA_DEV_ENV_JAVA_OPTS");
    }

    @Test
    void wrapperScript_containsInstallSuggestions()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        assertThat(content)
                .contains("SDKMAN")
                .contains("Homebrew")
                .contains("adoptium.net");
    }

    @Test
    void wrapperScript_containsVersionMismatchError()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        assertThat(content)
                .contains("print_version_mismatch");
    }

    @Test
    void wrapperScript_containsJarResolution()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        assertThat(content)
                .contains("resolve_script_dir")
                .contains("jar_path");
    }

    @Test
    void wrapperScript_forwardsArguments()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        assertThat(content).contains("\"$@\"");
    }

    @Test
    void wrapperScript_usesExecForProcess()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        assertThat(content).contains("exec \"$java_cmd\"");
    }

    @Test
    void wrapperScript_exitsOnJavaNotFound()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        assertThat(content)
                .contains("print_java_not_found");
    }

    @Test
    void wrapperScript_containsJarName()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        assertThat(content)
                .contains("ia-dev-env-2.0.0-SNAPSHOT.jar");
    }

    @Test
    void wrapperScript_handlesLegacyJavaVersion()
            throws IOException {
        String content = Files.readString(SCRIPT_PATH);
        // Handles legacy 1.x format (e.g., Java 1.8)
        assertThat(content)
                .contains("\"$version\" = \"1\"");
    }
}
