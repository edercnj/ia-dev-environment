/**
 * Driven Adapter — file-based checkpoint persistence.
 *
 * <p>Implements the checkpoint store output port. Provides
 * durable state persistence for generation progress, enabling
 * resume-after-failure and incremental generation workflows.
 *
 * <p><strong>Dependency restrictions (RULE-002):</strong>
 * <ul>
 *   <li>MUST implement a {@code domain.port.output} interface</li>
 *   <li>MAY use Java NIO for file operations</li>
 *   <li>MAY depend on {@code domain.model} for checkpoint
 *       data structures</li>
 *   <li>MUST NOT depend on other adapters or the CLI layer</li>
 * </ul>
 *
 * @see dev.iadev.domain.port.output
 */
package dev.iadev.infrastructure.adapter.output.checkpoint;
