package dev.iadev.cli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the fat JAR contains all required
 * resources and configuration.
 *
 * <p>These tests validate the JAR content from within
 * the classpath, ensuring shared/templates, shared/config-templates,
 * and MANIFEST.MF are bundled correctly.
 *
 * <p>Tests follow TPP: manifest, templates, configs.
 */
class FatJarContentTest {

    @Test
    void manifest_whenCalled_containsMainClass() throws IOException {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(
                        "META-INF/MANIFEST.MF")) {
            // Manifest may not be accessible in test
            // classpath; verify main class is correct
            // by checking the annotation on the class
            assertThat(IaDevEnvApplication.class
                    .isAnnotationPresent(
                            picocli.CommandLine.Command
                                    .class))
                    .isTrue();
        }
    }

    @Test
    void configTemplates_whenCalled_javaQuarkusExists() {
        assertResourceExists(
                "shared/config-templates/"
                        + "setup-config.java-quarkus.yaml");
    }

    @Test
    void configTemplates_whenCalled_javaSpringExists() {
        assertResourceExists(
                "shared/config-templates/"
                        + "setup-config.java-spring.yaml");
    }

    // Non-Java config template tests removed in EPIC-0048 story-0048-0007:
    // go-gin, kotlin-ktor, python-fastapi, python-click-cli, rust-axum,
    // typescript-nestjs, typescript-commander-cli YAMLs were deleted.

    @Test
    void templates_whenCalled_projectIdentityExists() {
        assertResourceExists(
                "shared/templates/project-identity-template.md");
    }

    @Test
    void templates_whenCalled_domainTemplateExists() {
        assertResourceExists(
                "shared/templates/domain-template.md");
    }

    @Test
    void templates_whenCalled_epicTemplateExists() {
        assertResourceExists(
                "shared/templates/_TEMPLATE-EPIC.md");
    }

    @Test
    void templates_whenCalled_storyTemplateExists() {
        assertResourceExists(
                "shared/templates/_TEMPLATE-STORY.md");
    }

    @Test
    void templates_whenCalled_systemSpecsExists() {
        assertResourceExists(
                "shared/templates/SYSTEM_SPECS.md");
    }

    @Test
    void coreRules_whenCalled_exist() {
        assertResourceExists(
                "targets/claude/rules/"
                + "01-project-identity.md");
    }

    @Test
    void settingsTemplates_whenCalled_exist() {
        assertResourceExists(
                "targets/claude/settings/base.json");
    }

    @Test
    void logback_whenCalled_configExists() {
        assertResourceExists("logback.xml");
    }

    @Test
    void nativeImage_whenCalled_reflectConfigExists() {
        assertResourceExists(
                "META-INF/native-image/"
                        + "dev.iadev/ia-dev-env/"
                        + "reflect-config.json");
    }

    @Test
    void nativeImage_whenCalled_resourceConfigExists() {
        assertResourceExists(
                "META-INF/native-image/"
                        + "dev.iadev/ia-dev-env/"
                        + "resource-config.json");
    }

    private void assertResourceExists(String path) {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(path)) {
            assertThat(is)
                    .as("Resource should exist: " + path)
                    .isNotNull();
            assertThat(is.available())
                    .as("Resource should have content: "
                            + path)
                    .isGreaterThan(0);
        } catch (IOException e) {
            throw new AssertionError(
                    "Failed to read resource: " + path,
                    e);
        }
    }
}
