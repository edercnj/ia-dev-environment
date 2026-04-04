/**
 * Immutable entities and value objects of the domain model.
 *
 * <p>This package contains the core domain types that represent
 * business concepts. All types must be immutable (records or
 * classes with final fields) and carry no framework annotations.
 *
 * <p><strong>Dependency restrictions (RULE-001, RULE-004):</strong>
 * <ul>
 *   <li>MUST NOT import any framework class (Picocli, Pebble,
 *       Jackson, SnakeYAML, JLine)</li>
 *   <li>MUST NOT depend on application, infrastructure,
 *       or adapter packages</li>
 *   <li>MAY depend only on Java standard library and other
 *       {@code domain.*} packages</li>
 * </ul>
 *
 * @see dev.iadev.domain.port.input
 * @see dev.iadev.domain.port.output
 */
package dev.iadev.domain.model;
