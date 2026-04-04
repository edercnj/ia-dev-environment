/**
 * Dependency resolution and component graph.
 *
 * <p>Contains the logic for resolving dependencies between
 * generation components and computing execution order via
 * directed acyclic graph (DAG) traversal.
 *
 * <p><strong>Dependency restrictions (RULE-001):</strong>
 * <ul>
 *   <li>MAY depend on {@code domain.model} types</li>
 *   <li>MUST NOT depend on infrastructure or adapter packages</li>
 *   <li>MUST NOT import framework classes</li>
 * </ul>
 *
 * @see dev.iadev.application.assembler
 */
package dev.iadev.application.dag;
