package dev.iadev.domain.stack;

/**
 * Command set for a language and build tool combination.
 *
 * <p>Contains all commands needed to compile, build, test, and measure
 * coverage for a specific language/build-tool pair, plus metadata about
 * the file extension, build file name, and package manager.</p>
 *
 * @param compileCmd the compilation command
 * @param buildCmd the build/package command
 * @param testCmd the test execution command
 * @param coverageCmd the coverage report command
 * @param fileExtension the source file extension (e.g. ".java", ".ts")
 * @param buildFile the build descriptor file (e.g. "pom.xml", "package.json")
 * @param packageManager the package manager name (e.g. "maven", "npm")
 */
public record LanguageCommandSet(
        String compileCmd,
        String buildCmd,
        String testCmd,
        String coverageCmd,
        String fileExtension,
        String buildFile,
        String packageManager) {
}
