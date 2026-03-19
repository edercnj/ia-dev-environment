package dev.iadev.domain.stack;

import java.util.List;

/**
 * Computed stack values derived from a ProjectConfig.
 *
 * <p>Immutable value object containing all resolved build commands,
 * Docker image, health path, port, project type, and protocols.
 * Produced by {@link StackResolver#resolve}.</p>
 *
 * @param compileCmd the compilation command
 * @param buildCmd the build/package command
 * @param testCmd the test execution command
 * @param coverageCmd the coverage report command
 * @param fileExtension the source file extension (e.g. ".java", ".ts")
 * @param buildFile the build descriptor file (e.g. "pom.xml", "package.json")
 * @param packageManager the package manager name (e.g. "maven", "npm")
 * @param defaultPort the default port for the framework
 * @param healthPath the health check endpoint path
 * @param dockerBaseImage the Docker base image for the language
 * @param nativeSupported whether GraalVM native build is supported
 * @param projectType the derived project type (api, worker, library, cli)
 * @param protocols the derived protocol spec names (e.g. openapi, proto3)
 */
public record ResolvedStack(
        String compileCmd,
        String buildCmd,
        String testCmd,
        String coverageCmd,
        String fileExtension,
        String buildFile,
        String packageManager,
        int defaultPort,
        String healthPath,
        String dockerBaseImage,
        boolean nativeSupported,
        String projectType,
        List<String> protocols) {

    /**
     * Compact constructor enforcing immutability of the protocols list.
     */
    public ResolvedStack {
        protocols = List.copyOf(protocols);
    }
}
