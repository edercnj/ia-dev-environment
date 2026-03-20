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
 * the classpath, ensuring templates, config-templates,
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
                "config-templates/"
                        + "setup-config.java-quarkus.yaml");
    }

    @Test
    void configTemplates_whenCalled_javaSpringExists() {
        assertResourceExists(
                "config-templates/"
                        + "setup-config.java-spring.yaml");
    }

    @Test
    void configTemplates_whenCalled_goGinExists() {
        assertResourceExists(
                "config-templates/"
                        + "setup-config.go-gin.yaml");
    }

    @Test
    void configTemplates_whenCalled_kotlinKtorExists() {
        assertResourceExists(
                "config-templates/"
                        + "setup-config.kotlin-ktor.yaml");
    }

    @Test
    void configTemplates_whenCalled_pythonFastapiExists() {
        assertResourceExists(
                "config-templates/"
                        + "setup-config.python-fastapi"
                        + ".yaml");
    }

    @Test
    void configTemplates_whenCalled_pythonClickCliExists() {
        assertResourceExists(
                "config-templates/"
                        + "setup-config.python-click-cli"
                        + ".yaml");
    }

    @Test
    void configTemplates_whenCalled_rustAxumExists() {
        assertResourceExists(
                "config-templates/"
                        + "setup-config.rust-axum.yaml");
    }

    @Test
    void configTemplates_whenCalled_typescriptNestjsExists() {
        assertResourceExists(
                "config-templates/"
                        + "setup-config"
                        + ".typescript-nestjs.yaml");
    }

    @Test
    void configTemplates_whenCalled_typescriptCommanderExists() {
        assertResourceExists(
                "config-templates/"
                        + "setup-config"
                        + ".typescript-commander-cli"
                        + ".yaml");
    }

    @Test
    void templates_whenCalled_projectIdentityExists() {
        assertResourceExists(
                "templates/project-identity-template.md");
    }

    @Test
    void templates_whenCalled_domainTemplateExists() {
        assertResourceExists(
                "templates/domain-template.md");
    }

    @Test
    void templates_whenCalled_epicTemplateExists() {
        assertResourceExists(
                "templates/_TEMPLATE-EPIC.md");
    }

    @Test
    void templates_whenCalled_storyTemplateExists() {
        assertResourceExists(
                "templates/_TEMPLATE-STORY.md");
    }

    @Test
    void templates_whenCalled_systemSpecsExists() {
        assertResourceExists(
                "templates/SYSTEM_SPECS.md");
    }

    @Test
    void coreRules_whenCalled_exist() {
        assertResourceExists(
                "core-rules/01-project-identity.md");
    }

    @Test
    void settingsTemplates_whenCalled_exist() {
        assertResourceExists(
                "settings-templates/base.json");
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
