/**
 * Domain model data classes (Java records).
 *
 * <p>Contains the 17 configuration records that represent the project
 * configuration structure. All records provide a {@code fromMap()} factory
 * method for YAML deserialization (RULE-003).
 *
 * <p>This package must NOT import any external framework classes
 * (Picocli, Pebble, Jackson, SnakeYAML, JLine) per RULE-007.
 */
package dev.iadev.model;
