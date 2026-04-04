/**
 * Assemblers — generation pipeline orchestrators.
 *
 * <p>Assemblers coordinate output ports to produce artifact files.
 * Each assembler handles a specific category of output (rules,
 * skills, agents, etc.) and implements a uniform interface for
 * pipeline orchestration.
 *
 * <p><strong>Dependency restrictions (RULE-001):</strong>
 * <ul>
 *   <li>MAY depend on {@code domain.model} and
 *       {@code domain.port.output}</li>
 *   <li>MUST NOT depend on infrastructure or adapter packages
 *       directly</li>
 *   <li>MUST NOT import framework classes</li>
 * </ul>
 *
 * @see dev.iadev.domain.port.output
 * @see dev.iadev.application.factory
 */
package dev.iadev.application.assembler;
