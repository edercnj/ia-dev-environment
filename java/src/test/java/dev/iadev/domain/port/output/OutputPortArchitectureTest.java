package dev.iadev.domain.port.output;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ArchUnit tests verifying output port interface rules.
 *
 * <p>Validates that all types in {@code domain.port.output} are
 * pure interfaces with no framework dependencies, conforming
 * to RULE-002 (Ports as Contracts).</p>
 */
class OutputPortArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("dev.iadev");
    }

    @Nested
    @DisplayName("Output Port Interface Rules")
    class InterfaceRules {

        @Test
        @DisplayName("All types in domain.port.output should be interfaces")
        void outputPortsShouldBeInterfaces() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..domain.port.output..")
                    .and().areNotPackagePrivate()
                    .should().beInterfaces()
                    .because(
                            "Output Ports are contracts "
                            + "— they must be interfaces (RULE-002)");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Output ports should not depend on framework types")
        void outputPortsShouldNotDependOnFrameworks() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain.port.output..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.fasterxml.jackson..",
                            "org.yaml.snakeyaml..",
                            "io.pebbletemplates..",
                            "info.picocli..",
                            "org.slf4j..",
                            "ch.qos.logback.."
                    )
                    .because(
                            "Output Ports must be framework-free "
                            + "— domain purity (RULE-001)");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Output ports should only depend on domain model or standard library")
        void outputPortsShouldOnlyDependOnDomainModel() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain.port.output..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "dev.iadev.adapter..",
                            "dev.iadev.infrastructure..",
                            "dev.iadev.application..",
                            "dev.iadev.cli..",
                            "dev.iadev.config..",
                            "dev.iadev.application.assembler..",
                            "dev.iadev.template..",
                            "dev.iadev.checkpoint..",
                            "dev.iadev.progress..",
                            "dev.iadev.util..",
                            "dev.iadev.smoke..",
                            "dev.iadev.exception.."
                    )
                    .because(
                            "Output Ports may only reference "
                            + "domain.model types or standard library");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Output Port Existence Verification")
    class ExistenceVerification {

        @Test
        @DisplayName("Four output port interfaces should exist")
        void fourOutputPortsShouldExist() {
            var outputPortClasses = importedClasses
                    .stream()
                    .filter(jc -> jc.getPackageName()
                            .equals("dev.iadev.domain.port.output"))
                    .filter(jc -> jc.isInterface())
                    .filter(jc -> !jc.getSimpleName()
                            .equals("package-info"))
                    .toList();

            assertThat(outputPortClasses)
                    .hasSize(4)
                    .extracting(jc -> jc.getSimpleName())
                    .containsExactlyInAnyOrder(
                            "StackProfileRepository",
                            "TemplateRenderer",
                            "FileSystemWriter",
                            "ProgressReporter"
                    );
        }
    }

    @Nested
    @DisplayName("Method Signature Verification")
    class MethodSignatureVerification {

        @Test
        @DisplayName("StackProfileRepository covers findAll, findByName, exists")
        void stackProfileRepository_methodSignatures() {
            var methods = getInterfaceMethodNames(
                    StackProfileRepository.class);

            assertThat(methods).containsExactlyInAnyOrder(
                    "findAll", "findByName", "exists");
        }

        @Test
        @DisplayName("TemplateRenderer covers render, templateExists")
        void templateRenderer_methodSignatures() {
            var methods = getInterfaceMethodNames(
                    TemplateRenderer.class);

            assertThat(methods).containsExactlyInAnyOrder(
                    "render", "templateExists");
        }

        @Test
        @DisplayName("FileSystemWriter covers writeFile, createDirectory, exists, copyResource")
        void fileSystemWriter_methodSignatures() {
            var methods = getInterfaceMethodNames(
                    FileSystemWriter.class);

            assertThat(methods).containsExactlyInAnyOrder(
                    "writeFile", "createDirectory",
                    "exists", "copyResource");
        }

        @Test
        @DisplayName("ProgressReporter covers reportStart, reportProgress, reportComplete, reportError")
        void progressReporter_methodSignatures() {
            var methods = getInterfaceMethodNames(
                    ProgressReporter.class);

            assertThat(methods).containsExactlyInAnyOrder(
                    "reportStart", "reportProgress",
                    "reportComplete", "reportError");
        }

        private java.util.List<String> getInterfaceMethodNames(
                Class<?> iface) {
            return java.util.Arrays.stream(iface.getDeclaredMethods())
                    .map(java.lang.reflect.Method::getName)
                    .toList();
        }
    }
}
