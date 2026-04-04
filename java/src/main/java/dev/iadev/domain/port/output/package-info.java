/**
 * Output Ports — interfaces for infrastructure resources.
 *
 * <p>Output ports define contracts that driven adapters must
 * implement. They abstract away external concerns such as
 * file system access, template rendering, configuration
 * loading, checkpoint persistence, and progress reporting.
 *
 * <p><strong>Dependency restrictions (RULE-001, RULE-002):</strong>
 * <ul>
 *   <li>MUST be interfaces only — no concrete classes</li>
 *   <li>MUST NOT import infrastructure or adapter packages</li>
 *   <li>MAY reference {@code domain.model} types in method
 *       signatures</li>
 * </ul>
 *
 * @see dev.iadev.domain.service
 * @see dev.iadev.infrastructure.adapter.output
 */
package dev.iadev.domain.port.output;
