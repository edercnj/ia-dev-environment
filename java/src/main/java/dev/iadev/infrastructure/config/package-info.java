/**
 * Composition Root — manual dependency wiring.
 *
 * <p>This package is the single place where all dependencies
 * are assembled. It creates concrete adapter instances, injects
 * them into domain services, and exposes fully-wired use case
 * implementations to the CLI layer.
 *
 * <p><strong>Dependency restrictions (RULE-005):</strong>
 * <ul>
 *   <li>MAY depend on ALL packages (domain, application,
 *       infrastructure adapters)</li>
 *   <li>This is the ONLY package allowed to instantiate
 *       concrete adapter implementations</li>
 *   <li>Domain and application packages MUST NOT depend on
 *       this package</li>
 * </ul>
 *
 * @see dev.iadev.domain.port.input
 * @see dev.iadev.domain.port.output
 */
package dev.iadev.infrastructure.config;
