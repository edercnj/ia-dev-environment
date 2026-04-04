package dev.iadev.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Standalone audit runner (not a JUnit test) that evaluates
 * all hexagonal architecture rules against the current codebase
 * and prints violations for the baseline report.
 *
 * <p>Run via: {@code java -cp ... dev.iadev.architecture.HexagonalArchitectureBaselineAudit}
 * or invoke {@link #main(String[])} from a test helper.
 */
final class HexagonalArchitectureBaselineAudit {

    private HexagonalArchitectureBaselineAudit() {
    }

    static Map<String, EvaluationResult> runAudit() {
        JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("dev.iadev");

        Map<String, EvaluationResult> results = new LinkedHashMap<>();

        results.put("RULE-001a: domainShouldNotDependOnInfrastructure",
            buildDomainInfraRule().evaluate(classes));

        results.put("RULE-001b: domainShouldNotDependOnApplication",
            buildDomainAppRule().evaluate(classes));

        results.put("RULE-004: domainModelShouldNotHaveFrameworkAnnotations",
            buildFrameworkAnnotationRule().evaluate(classes));

        results.put("RULE-002: outputPortsShouldBeInterfaces",
            buildOutputPortsRule().evaluate(classes));

        results.put("RULE-003a: inputPortsShouldBeInterfaces",
            buildInputPortsRule().evaluate(classes));

        results.put("RULE-003b: inputPortsShouldOnlyDependOnDomainModel",
            buildInputPortDependencyRule().evaluate(classes));

        results.put("RULE-003c: cliShouldOnlyAccessInputPorts",
            buildCliAccessRule().evaluate(classes));

        results.put("RULE-005: compositionRootShouldBeUnique",
            buildCompositionRootRule().evaluate(classes));

        return results;
    }

    static ArchRule buildDomainInfraRule() {
        return noClasses()
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
            );
    }

    static ArchRule buildDomainAppRule() {
        return noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application..");
    }

    static ArchRule buildFrameworkAnnotationRule() {
        return noClasses()
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
            );
    }

    static ArchRule buildOutputPortsRule() {
        return classes()
            .that().resideInAPackage("..domain.port.output..")
            .should().beInterfaces()
            .allowEmptyShould(true);
    }

    static ArchRule buildInputPortsRule() {
        return classes()
            .that().resideInAPackage("..domain.port.input..")
            .should().beInterfaces()
            .allowEmptyShould(true);
    }

    static ArchRule buildInputPortDependencyRule() {
        return classes()
            .that().resideInAPackage("..domain.port.input..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..domain.model..",
                "..domain.port.input..",
                "java..",
                "javax.."
            )
            .allowEmptyShould(true);
    }

    static ArchRule buildCliAccessRule() {
        return noClasses()
            .that().resideInAPackage("..cli..")
            .should().dependOnClassesThat()
            .resideInAPackage("..domain..engine..")
            .allowEmptyShould(true);
    }

    static ArchRule buildCompositionRootRule() {
        return noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..config..");
    }

    public static void main(String[] args) {
        Map<String, EvaluationResult> results = runAudit();
        int totalViolations = 0;

        for (Map.Entry<String, EvaluationResult> entry : results.entrySet()) {
            EvaluationResult result = entry.getValue();
            int count = result.getFailureReport()
                .getDetails().size();
            totalViolations += count;

            System.out.printf("%n=== %s ===%n", entry.getKey());
            System.out.printf("Violations: %d%n", count);
            if (count > 0) {
                result.getFailureReport().getDetails()
                    .forEach(d -> System.out.printf("  - %s%n", d));
            }
        }

        System.out.printf("%n--- Total violations: %d ---%n",
            totalViolations);
    }
}
