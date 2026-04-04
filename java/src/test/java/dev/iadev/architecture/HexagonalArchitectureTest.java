package dev.iadev.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Hexagonal architecture rules enforced via ArchUnit.
 *
 * <p>Each rule starts as {@code @Disabled} and is activated
 * progressively as migration stories complete. The baseline
 * violations are documented in
 * {@code docs/architecture/archunit-baseline-report.md}.
 */
class HexagonalArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("dev.iadev");
    }

    // --- RULE-001: Domain isolation ---

    @Test
    @Disabled("AS-IS: domain.model.MapHelper imports "
        + "dev.iadev.exception — resolve in story-0015-0006+")
    @DisplayName("RULE-001: Domain must not depend on "
        + "infrastructure packages")
    void domainShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "dev.iadev.application.assembler..",
                "dev.iadev.cli..",
                "dev.iadev.config..",
                "dev.iadev.template..",
                "dev.iadev.checkpoint..",
                "dev.iadev.progress..",
                "dev.iadev.smoke..",
                "dev.iadev.util..",
                "dev.iadev.model..",
                "dev.iadev.exception.."
            )
            .because("RULE-001: Domain must not depend on "
                + "infrastructure or adapter packages");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("RULE-001: Domain must not depend on "
        + "the application layer")
    void domainShouldNotDependOnApplication() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application..")
            .because("RULE-001: Domain must not depend on "
                + "the application layer");

        rule.check(importedClasses);
    }

    // --- RULE-004: Domain model purity ---

    @Test
    @DisplayName("RULE-004: Domain model must have zero "
        + "framework dependencies")
    void domainModelShouldNotHaveFrameworkAnnotations() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.fasterxml.jackson..",
                "io.pebbletemplates..",
                "info.picocli..",
                "org.yaml.snakeyaml..",
                "jakarta..",
                "javax.inject..",
                "org.springframework.."
            )
            .because("RULE-004: Domain model must have zero "
                + "framework or serialization dependencies");

        rule.check(importedClasses);
    }

    // --- RULE-002: Output ports ---

    @Test
    @DisplayName("RULE-002: Output ports must be interfaces")
    void outputPortsShouldBeInterfaces() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain.port.output..")
            .and().doNotHaveSimpleName("package-info")
            .should().beInterfaces()
            .because("RULE-002: Output ports must be interfaces "
                + "so adapters can implement them");

        rule.check(importedClasses);
    }

    // --- RULE-003: Input ports ---

    @Test
    @DisplayName("RULE-003: Input ports must be interfaces")
    void inputPortsShouldBeInterfaces() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain.port.input..")
            .and().doNotHaveSimpleName("package-info")
            .should().beInterfaces()
            .because("RULE-003: Input ports are Use Case contracts "
                + "— they must be interfaces");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("RULE-003: Input ports should only depend on "
        + "domain model or standard library")
    void inputPortsShouldOnlyDependOnDomainModel() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain.port.input..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..domain.model..",
                "..domain.port.input..",
                "java..",
                "javax.."
            )
            .because("Input Ports must only reference "
                + "domain model types or standard library");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("RULE-003: CLI must only access input ports")
    void cliShouldOnlyAccessInputPorts() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..cli..")
            .should().dependOnClassesThat()
            .resideInAPackage("..domain..engine..")
            .because("RULE-003: CLI adapter must only access "
                + "input ports, not domain internals");

        rule.check(importedClasses);
    }

    // --- RULE-005: Composition root ---

    @Test
    @Disabled("Activate after story-0015-0014 — "
        + "AS-IS: composition root not yet defined")
    @DisplayName("RULE-005: Domain must not reference config")
    void compositionRootShouldBeUnique() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..config..")
            .because("RULE-005: Only the composition root (config) "
                + "should wire dependencies; domain must not "
                + "reference config");

        rule.check(importedClasses);
    }
}
