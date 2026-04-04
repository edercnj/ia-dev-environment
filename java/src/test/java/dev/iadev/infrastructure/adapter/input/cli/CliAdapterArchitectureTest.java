package dev.iadev.infrastructure.adapter.input.cli;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax
        .ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests for CLI Input Adapter compliance.
 *
 * <p>Verifies RULE-003: CLI adapter must use only Input
 * Ports, never domain services or engines directly.</p>
 *
 * <p>Verifies GK-3: CLI adapter must not import
 * domain.service classes.</p>
 *
 * <p>Verifies GK-5: Picocli annotations exist only in the
 * adapter CLI package, not in domain or application.</p>
 */
@DisplayName("CLI Adapter Architecture Rules")
class CliAdapterArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined
                                .DO_NOT_INCLUDE_TESTS)
                .importPackages("dev.iadev");
    }

    @Nested
    @DisplayName("RULE-003: CLI uses only Input Ports")
    class CliInputPortCompliance {

        @Test
        @DisplayName("cliAdapter_shouldNotDependOn"
                + "_domainServicePackage")
        void cliAdapter_shouldNotDependOnDomainService() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(
                            "..infrastructure.adapter"
                                    + ".input.cli..")
                    .should().dependOnClassesThat()
                    .resideInAPackage(
                            "..domain.service..")
                    .because("RULE-003: CLI adapter must use "
                            + "only Input Ports, not Domain "
                            + "Services directly");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("cliAdapter_shouldNotDependOn"
                + "_domainEnginePackage")
        void cliAdapter_shouldNotDependOnDomainEngine() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(
                            "..infrastructure.adapter"
                                    + ".input.cli..")
                    .should().dependOnClassesThat()
                    .resideInAPackage(
                            "..domain..engine..")
                    .because("RULE-003: CLI adapter must not "
                            + "access domain engines directly");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("cliAdapter_shouldNotDependOn"
                + "_outputAdapters")
        void cliAdapter_shouldNotDependOnOutputAdapters() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(
                            "..infrastructure.adapter"
                                    + ".input.cli..")
                    .should().dependOnClassesThat()
                    .resideInAPackage(
                            "..infrastructure.adapter"
                                    + ".output..")
                    .because("RULE-003: Input adapter must "
                            + "not depend on output adapters");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("GK-5: Picocli annotations isolation")
    class PicocliAnnotationIsolation {

        @Test
        @DisplayName("domainPackage_shouldNotDependOn"
                + "_picocliFramework")
        void domain_shouldNotDependOnPicocli() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..picocli..")
                    .because("GK-5: Picocli annotations "
                            + "must not exist in domain");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("applicationPackage_shouldNotDependOn"
                + "_picocliFramework")
        void application_shouldNotDependOnPicocli() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(
                            "..application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..picocli..")
                    .because("GK-5: Picocli annotations "
                            + "must not exist in application");

            rule.check(importedClasses);
        }
    }
}
