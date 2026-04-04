/**
 * Input Ports — use case interfaces exposed to the outside world.
 *
 * <p>Input ports define the contract that driving adapters (CLI,
 * REST, etc.) use to invoke domain behavior. Each interface
 * represents a single use case following the Interface Segregation
 * Principle.
 *
 * <p><strong>Dependency restrictions (RULE-001, RULE-003):</strong>
 * <ul>
 *   <li>MUST be interfaces only — no concrete classes</li>
 *   <li>MUST NOT import infrastructure or adapter packages</li>
 *   <li>MAY reference {@code domain.model} types in method
 *       signatures</li>
 * </ul>
 *
 * @see dev.iadev.domain.service
 * @see dev.iadev.infrastructure.adapter.input.cli
 */
package dev.iadev.domain.port.input;
