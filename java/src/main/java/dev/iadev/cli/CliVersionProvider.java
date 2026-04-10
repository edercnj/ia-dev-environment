package dev.iadev.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import picocli.CommandLine.IVersionProvider;

/**
 * Picocli version provider that reads the application
 * version from {@code dev/iadev/version.properties}, a
 * resource filtered by Maven from {@code ${project.version}}
 * at build time. Keeps the CLI version single-sourced from
 * {@code pom.xml}.
 */
public final class CliVersionProvider
        implements IVersionProvider {

    static final String RESOURCE_PATH =
            "/dev/iadev/version.properties";
    static final String VERSION_KEY = "version";
    static final String UNKNOWN_VERSION = "unknown";

    @Override
    public String[] getVersion() throws IOException {
        Properties props = new Properties();
        try (InputStream in = CliVersionProvider.class
                .getResourceAsStream(RESOURCE_PATH)) {
            if (in != null) {
                props.load(in);
            }
        }
        String version = props.getProperty(
                VERSION_KEY, UNKNOWN_VERSION);
        return new String[] {
                "ia-dev-env " + version
        };
    }
}
