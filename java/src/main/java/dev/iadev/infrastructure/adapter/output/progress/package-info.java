/**
 * Driven Adapter — console progress reporting.
 *
 * <p>Implements the progress reporter output port. Displays
 * real-time generation progress to the user via console output,
 * including step counts, status indicators, and summary reports.
 *
 * <p><strong>Dependency restrictions (RULE-002):</strong>
 * <ul>
 *   <li>MUST implement a {@code domain.port.output} interface</li>
 *   <li>MAY use JLine or System.out for console output</li>
 *   <li>MAY depend on {@code domain.model} for progress
 *       data structures</li>
 *   <li>MUST NOT depend on other adapters or the CLI layer</li>
 * </ul>
 *
 * @see dev.iadev.domain.port.output
 */
package dev.iadev.infrastructure.adapter.output.progress;
