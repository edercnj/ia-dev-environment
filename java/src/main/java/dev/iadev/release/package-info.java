/**
 * Release orchestration domain package for story-0039-0001 (Auto-version
 * detection via Conventional Commits).
 *
 * <p>Contains the pure-domain primitives {@link dev.iadev.release.SemVer},
 * {@link dev.iadev.release.BumpType}, {@link dev.iadev.release.CommitCounts},
 * {@link dev.iadev.release.ConventionalCommitsParser}, and
 * {@link dev.iadev.release.VersionBumper}; plus the outbound adapter
 * {@link dev.iadev.release.GitTagReader} (Rule 04: adapter.outbound on a
 * port interface {@link dev.iadev.release.TagReader}).</p>
 *
 * <p>Algorithm summary per Conventional Commits v1.0.0:</p>
 * <ul>
 *   <li>{@code feat!}, {@code fix!}, or body containing {@code BREAKING CHANGE:} &rarr; MAJOR</li>
 *   <li>{@code feat:} (no {@code !}) &rarr; MINOR</li>
 *   <li>{@code fix:} or {@code perf:} &rarr; PATCH</li>
 *   <li>{@code docs:}, {@code chore:}, {@code test:}, {@code refactor:},
 *       {@code style:}, {@code build:}, {@code ci:} &rarr; ignored</li>
 * </ul>
 *
 * <p>Domain purity: none of the classes in this package import framework or I/O
 * libraries except the {@link dev.iadev.release.GitTagReader} adapter, which
 * uses {@code java.lang.ProcessBuilder} with a fixed argv (Rule 06 — no shell
 * expansion, no user-controlled concat).</p>
 */
package dev.iadev.release;
