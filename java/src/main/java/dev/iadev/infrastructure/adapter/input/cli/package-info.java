/**
 * Driving Adapter — Picocli CLI commands.
 *
 * <p>This package contains the command-line interface handlers
 * that translate user input into calls to domain input ports.
 * Commands use Picocli annotations and delegate all business
 * logic to the application layer via input port interfaces.
 *
 * <p><strong>Dependency restrictions (RULE-003):</strong>
 * <ul>
 *   <li>MAY import Picocli framework classes</li>
 *   <li>MAY depend on {@code domain.port.input} interfaces</li>
 *   <li>MUST NOT access domain internals (services, engines)
 *       directly</li>
 *   <li>MUST NOT depend on output adapters</li>
 * </ul>
 *
 * @see dev.iadev.domain.port.input
 */
package dev.iadev.infrastructure.adapter.input.cli;
