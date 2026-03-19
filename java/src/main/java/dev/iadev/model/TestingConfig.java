package dev.iadev.model;

import java.util.Map;

/**
 * Represents the testing configuration section.
 *
 * <p>Provides default coverage thresholds of 95% line and 90% branch coverage,
 * matching the project quality gates and TypeScript implementation.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("coverage_line", 80, "smoke_tests", false);
 * TestingConfig cfg = TestingConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param smokeTests whether smoke tests are enabled (default: true)
 * @param contractTests whether contract tests are enabled (default: false)
 * @param performanceTests whether performance tests are enabled (default: true)
 * @param coverageLine the line coverage threshold percentage (default: 95)
 * @param coverageBranch the branch coverage threshold percentage (default: 90)
 */
public record TestingConfig(
        boolean smokeTests,
        boolean contractTests,
        boolean performanceTests,
        int coverageLine,
        int coverageBranch) {

    /**
     * Creates a TestingConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new TestingConfig instance with defaults for missing values
     */
    public static TestingConfig fromMap(Map<String, Object> map) {
        return new TestingConfig(
                MapHelper.optionalBoolean(map, "smoke_tests", true),
                MapHelper.optionalBoolean(map, "contract_tests", false),
                MapHelper.optionalBoolean(map, "performance_tests", true),
                MapHelper.optionalInt(map, "coverage_line", 95),
                MapHelper.optionalInt(map, "coverage_branch", 90)
        );
    }
}
