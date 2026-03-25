/**
 * Smoke test infrastructure for validating pipeline output.
 *
 * <p>Contains the expected artifacts manifest model
 * ({@link dev.iadev.smoke.ProfileArtifacts},
 * {@link dev.iadev.smoke.ExpectedArtifacts}) and the
 * manifest generator
 * ({@link dev.iadev.smoke.ExpectedArtifactsGenerator}).
 *
 * <p>The manifest declares per-profile expectations (file
 * counts, directories, categories) enabling smoke tests
 * to validate pipeline output against concrete
 * expectations (RULE-005).
 */
package dev.iadev.smoke;
