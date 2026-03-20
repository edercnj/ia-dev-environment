/**
 * Custom exception hierarchy for the ia-dev-env application.
 *
 * <p>Provides specific exception types for CLI errors, configuration
 * parsing/validation errors, pipeline failures, checkpoint issues,
 * resource resolution, and generation cancellation.
 * All exceptions carry contextual information about the error cause.</p>
 *
 * <h2>Exception Location Convention</h2>
 * <ul>
 *   <li><strong>{@code exception/}</strong> — application-level exceptions
 *       used across multiple layers (e.g., {@code ResourceNotFoundException},
 *       {@code GenerationCancelledException}).</li>
 *   <li><strong>{@code domain/{context}/}</strong> — domain-specific
 *       exceptions used only within a single bounded context
 *       (e.g., {@code MapParseException} in
 *       {@code domain/implementationmap/}).</li>
 * </ul>
 */
package dev.iadev.exception;
