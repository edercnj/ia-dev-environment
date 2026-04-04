/**
 * Domain Services — use case implementations that orchestrate
 * input and output ports.
 *
 * <p>Each service implements one or more input port interfaces
 * and delegates side effects to output ports via constructor
 * injection. Services contain orchestration logic but no
 * infrastructure concerns.
 *
 * <p><strong>Dependency restrictions (RULE-001):</strong>
 * <ul>
 *   <li>MUST NOT import infrastructure or adapter packages</li>
 *   <li>MUST NOT import framework classes</li>
 *   <li>MAY depend on {@code domain.model}, {@code domain.port.input},
 *       and {@code domain.port.output}</li>
 * </ul>
 *
 * @see dev.iadev.domain.port.input
 * @see dev.iadev.domain.port.output
 */
package dev.iadev.domain.service;
