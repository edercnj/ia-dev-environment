package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;

import java.util.List;

/**
 * Evaluator that contributes conditional skill names based
 * on a single feature gate of the {@link ProjectConfig}.
 *
 * <p>Each implementation encapsulates the "should skills X,
 * Y, Z be generated when feature F is configured as V"
 * decision for exactly one feature, enabling OCP-compliant
 * extension: new features are added by creating a new
 * evaluator, not by modifying existing code.</p>
 *
 * @since EPIC-0048 (audit finding M-005)
 */
@FunctionalInterface
public interface SkillGateEvaluator {

    /**
     * Evaluates the gate and returns contributed skill names.
     *
     * @param config the project configuration
     * @return list of skill names this gate contributes;
     *         empty list when the gate is not active
     */
    List<String> evaluate(ProjectConfig config);
}
