/**
 * Assemblers that compose the artifact generation pipeline.
 *
 * <p>Each assembler implements the uniform {@link Assembler} interface
 * (RULE-004) to generate a specific category of output files (rules,
 * skills, agents, etc.). The {@link AssemblerPipeline} orchestrates
 * execution of 25 assemblers in the fixed order defined by RULE-005.</p>
 *
 * <p>Key components:
 * <ul>
 *   <li>{@link Assembler} — uniform contract for all assemblers</li>
 *   <li>{@link AssemblerPipeline} — orchestrator (RULE-005 order)</li>
 *   <li>{@link AssemblerTarget} — maps logical to physical dirs</li>
 *   <li>{@link PipelineOptions} — execution options (dry-run, etc.)</li>
 *   <li>{@link CopyHelpers} — template copy/render utilities</li>
 *   <li>{@link Consolidator} — file merging utilities</li>
 *   <li>{@link ConditionEvaluator} — feature gate evaluation</li>
 * </ul>
 */
package dev.iadev.assembler;
