/**
 * Driven Adapter — file system write operations.
 *
 * <p>Implements the file writing output port. Handles all
 * physical file system interactions including directory creation,
 * file writing, and path resolution.
 *
 * <p><strong>Dependency restrictions (RULE-002):</strong>
 * <ul>
 *   <li>MUST implement a {@code domain.port.output} interface</li>
 *   <li>MAY use Java NIO and standard I/O classes</li>
 *   <li>MAY depend on {@code domain.model} for file metadata</li>
 *   <li>MUST NOT depend on other adapters or the CLI layer</li>
 * </ul>
 *
 * @see dev.iadev.domain.port.output
 */
package dev.iadev.infrastructure.adapter.output.filesystem;
