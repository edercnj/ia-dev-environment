/**
 * Implementation Map parser and DAG computation module.
 *
 * <p>Transforms IMPLEMENTATION-MAP.md markdown tables into a directed
 * acyclic graph (DAG), validates structural integrity, computes
 * execution phases, and identifies the critical path.</p>
 *
 * <p>This package has zero framework dependencies (RULE-007).
 * It depends only on the Java standard library and
 * {@link dev.iadev.checkpoint.StoryStatus} /
 * {@link dev.iadev.checkpoint.ExecutionState} for executable story
 * filtering.</p>
 */
package dev.iadev.domain.implementationmap;
