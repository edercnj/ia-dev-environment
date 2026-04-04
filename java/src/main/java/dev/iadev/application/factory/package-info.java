/**
 * Generation context factories.
 *
 * <p>Responsible for creating and configuring the generation
 * context that assemblers use during artifact production. Factories
 * assemble domain objects and wire dependencies needed by the
 * application layer.
 *
 * <p><strong>Dependency restrictions (RULE-001):</strong>
 * <ul>
 *   <li>MAY depend on {@code domain.model} and
 *       {@code domain.port.*}</li>
 *   <li>MUST NOT depend on infrastructure or adapter packages
 *       directly</li>
 *   <li>MUST NOT import framework classes</li>
 * </ul>
 *
 * @see dev.iadev.application.assembler
 * @see dev.iadev.infrastructure.config
 */
package dev.iadev.application.factory;
