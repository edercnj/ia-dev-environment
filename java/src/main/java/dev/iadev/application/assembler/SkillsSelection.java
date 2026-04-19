package dev.iadev.application.assembler;

import dev.iadev.application.assembler.gates.ComplianceGate;
import dev.iadev.application.assembler.gates.InfraGate;
import dev.iadev.application.assembler.gates.InterfaceGate;
import dev.iadev.application.assembler.gates.PentestGate;
import dev.iadev.application.assembler.gates.ReviewGate;
import dev.iadev.application.assembler.gates.SecurityGate;
import dev.iadev.application.assembler.gates.SecurityScanningGate;
import dev.iadev.application.assembler.gates.SkillGateEvaluator;
import dev.iadev.application.assembler.gates.TestingGate;
import dev.iadev.domain.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure skill selection functions based on project config
 * feature gates.
 *
 * <p>Delegates each feature-gate decision to a dedicated
 * {@link SkillGateEvaluator} under the
 * {@code application.assembler.gates} package. Adding a new
 * feature = creating a new evaluator class and registering
 * it in {@link #EVALUATORS} — no edits to existing branching
 * logic (OCP).</p>
 *
 * <p>Knowledge-pack selection is delegated to
 * {@link KnowledgePackSelection}.</p>
 *
 * @see SkillsAssembler
 * @see KnowledgePackSelection
 * @see SkillGateEvaluator
 */
public final class SkillsSelection {

    private static final InterfaceGate INTERFACE_GATE =
            new InterfaceGate();
    private static final InfraGate INFRA_GATE = new InfraGate();
    private static final TestingGate TESTING_GATE =
            new TestingGate();
    private static final SecurityGate SECURITY_GATE =
            new SecurityGate();
    private static final SecurityScanningGate SCANNING_GATE =
            new SecurityScanningGate();
    private static final ComplianceGate COMPLIANCE_GATE =
            new ComplianceGate();
    private static final PentestGate PENTEST_GATE =
            new PentestGate();
    private static final ReviewGate REVIEW_GATE =
            new ReviewGate();

    private static final List<SkillGateEvaluator> EVALUATORS =
            List.of(
                    INTERFACE_GATE,
                    INFRA_GATE,
                    TESTING_GATE,
                    SECURITY_GATE,
                    SCANNING_GATE,
                    COMPLIANCE_GATE,
                    PENTEST_GATE,
                    REVIEW_GATE);

    private SkillsSelection() {
        // utility class
    }

    /**
     * Selects skills based on interface types.
     *
     * @param config the project configuration
     * @return list of conditional interface skill names
     */
    public static List<String> selectInterfaceSkills(
            ProjectConfig config) {
        return INTERFACE_GATE.evaluate(config);
    }

    /**
     * Selects skills based on infrastructure config.
     *
     * @param config the project configuration
     * @return list of conditional infrastructure skill names
     */
    public static List<String> selectInfraSkills(
            ProjectConfig config) {
        return INFRA_GATE.evaluate(config);
    }

    /**
     * Selects skills based on testing config.
     *
     * @param config the project configuration
     * @return list of conditional testing skill names
     */
    public static List<String> selectTestingSkills(
            ProjectConfig config) {
        return TESTING_GATE.evaluate(config);
    }

    /**
     * Selects skills based on security config.
     *
     * @param config the project configuration
     * @return list of conditional security skill names
     */
    public static List<String> selectSecuritySkills(
            ProjectConfig config) {
        return SECURITY_GATE.evaluate(config);
    }

    /**
     * Selects skills based on security scanning flags,
     * pentest, and quality gate provider.
     *
     * @param config the project configuration
     * @return list of conditional scanning skill names
     */
    public static List<String> selectSecurityScanningSkills(
            ProjectConfig config) {
        return SCANNING_GATE.evaluate(config);
    }

    /**
     * Selects skills based on compliance frameworks.
     *
     * @param config the project configuration
     * @return list of conditional compliance skill names
     */
    public static List<String> selectComplianceSkills(
            ProjectConfig config) {
        return COMPLIANCE_GATE.evaluate(config);
    }

    /**
     * Selects pentest skills based on security config.
     *
     * @param config the project configuration
     * @return list of conditional pentest skill names
     */
    public static List<String> selectPentestSkills(
            ProjectConfig config) {
        return PENTEST_GATE.evaluate(config);
    }

    /**
     * Selects review skills based on database, observability,
     * container, and architecture config.
     *
     * @param config the project configuration
     * @return list of conditional review skill names
     */
    public static List<String> selectReviewSkills(
            ProjectConfig config) {
        return REVIEW_GATE.evaluate(config);
    }

    /**
     * Evaluates every registered {@link SkillGateEvaluator}
     * and returns the aggregated list of conditional skill
     * names. Order follows the registry in {@link #EVALUATORS}
     * (iteration order is stable).
     *
     * @param config the project configuration
     * @return aggregated list of all conditional skill names
     */
    public static List<String> selectConditionalSkills(
            ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        for (SkillGateEvaluator evaluator : EVALUATORS) {
            skills.addAll(evaluator.evaluate(config));
        }
        return skills;
    }

    /**
     * Delegates to {@link KnowledgePackSelection}.
     *
     * @param config the project configuration
     * @return list of knowledge pack names to include
     */
    public static List<String> selectKnowledgePacks(
            ProjectConfig config) {
        return KnowledgePackSelection
                .selectKnowledgePacks(config);
    }

    /**
     * Checks if the architecture style supports DDD tactical
     * patterns (hexagonal, ddd, cqrs, clean).
     *
     * @param config the project configuration
     * @return true if architecture style is DDD-compatible
     */
    static boolean isHexagonalOrDdd(ProjectConfig config) {
        return ReviewGate.isHexagonalOrDdd(config);
    }
}
