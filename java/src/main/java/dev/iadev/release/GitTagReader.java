package dev.iadev.release;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Adapter (Rule 04, outbound) that shells to {@code git} using
 * {@link ProcessBuilder} with a fixed argv — no shell, no string
 * concatenation of user input.
 *
 * <p>Security posture (Rule 06, OWASP A03 Injection):</p>
 * <ul>
 *   <li>Repository path is canonicalised and validated to be an existing
 *       directory before any {@code git} call.</li>
 *   <li>Any {@code fromRef} argument passed to {@link #commitsSince(Optional)}
 *       is validated against {@link #SAFE_REF} — a character-class allow-list
 *       of {@code [A-Za-z0-9._/\-]+} — before being forwarded to {@code git
 *       log}. This accepts the broader git revision grammar the CLI needs
 *       (tags like {@code v1.2.3}, refs like {@code refs/tags/v1.2.3},
 *       relative specs like {@code HEAD~1}) while rejecting every shell
 *       metacharacter ({@code ; & | ` $ ( ) < > " ' \\ space}). Rejected
 *       values raise {@link IllegalArgumentException}. Note that this is
 *       deliberately looser than the SemVer literal grammar enforced by
 *       {@link SemVer#parse}, which governs {@code --version} arguments.</li>
 *   <li>{@code git} exit code 128 with stderr
 *       {@code fatal: No names found, cannot describe anything} is the
 *       documented signal for "no matching tag" and produces
 *       {@link Optional#empty()} from {@link #lastTag()}.</li>
 *   <li>Error messages surfaced to callers omit absolute filesystem paths and
 *       stack frames (Rule 06 — no internal path disclosure).</li>
 * </ul>
 */
public final class GitTagReader implements TagReader {

    private static final String TAG_GLOB = "v*";
    private static final Pattern SAFE_REF = Pattern.compile("^[A-Za-z0-9._/\\-]+$");
    static final String NO_TAGS_STDERR_MARKER = "fatal: No names found";

    private final Path repoDir;

    /**
     * @param repoDir path to a git working tree. Must exist and be a directory;
     *                will be canonicalised via {@link Path#toRealPath}.
     * @throws IllegalArgumentException if {@code repoDir} is not a directory.
     * @throws UncheckedIOException if {@code repoDir} cannot be canonicalised.
     */
    public GitTagReader(Path repoDir) {
        Objects.requireNonNull(repoDir, "repoDir");
        try {
            Path real = repoDir.toRealPath();
            if (!Files.isDirectory(real)) {
                throw new IllegalArgumentException("repoDir is not a directory");
            }
            this.repoDir = real;
        } catch (IOException e) {
            throw new UncheckedIOException("repoDir cannot be canonicalised", e);
        }
    }

    @Override
    public Optional<String> lastTag() {
        ProcessResult result = run(List.of("git", "describe", "--tags", "--abbrev=0", "--match", TAG_GLOB));
        if (result.exitCode == 0) {
            String tag = result.stdout.strip();
            return tag.isEmpty() ? Optional.empty() : Optional.of(tag);
        }
        if (result.stderr.contains(NO_TAGS_STDERR_MARKER)) {
            return Optional.empty();
        }
        throw new UncheckedIOException(new IOException(
                "git describe failed with exit " + result.exitCode));
    }

    @Override
    public List<String> commitsSince(Optional<String> fromRef) {
        Objects.requireNonNull(fromRef, "fromRef");
        List<String> argv = new ArrayList<>();
        argv.add("git");
        argv.add("log");
        argv.add("--no-merges");
        argv.add("--format=%s%n%b%x00");
        if (fromRef.isPresent()) {
            String ref = fromRef.get();
            if (!SAFE_REF.matcher(ref).matches()) {
                throw new IllegalArgumentException(
                        "fromRef contains unsafe characters");
            }
            argv.add(ref + "..HEAD");
        } else {
            argv.add("HEAD");
        }
        ProcessResult result = run(argv);
        if (result.exitCode != 0) {
            throw new UncheckedIOException(new IOException(
                    "git log failed with exit " + result.exitCode));
        }
        if (result.stdout.isBlank()) {
            return Collections.emptyList();
        }
        String[] rawEntries = result.stdout.split("\u0000");
        List<String> commits = new ArrayList<>(rawEntries.length);
        for (String raw : rawEntries) {
            String trimmed = raw.strip();
            if (!trimmed.isEmpty()) {
                commits.add(trimmed);
            }
        }
        return Collections.unmodifiableList(commits);
    }

    private ProcessResult run(List<String> argv) {
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(argv)
                    .directory(repoDir.toFile())
                    .redirectErrorStream(false);
            process = pb.start();
            // Drain stdout and stderr concurrently to avoid the OS-pipe-buffer
            // deadlock (git can write to stderr while we are still draining
            // stdout; if the stderr pipe fills, git blocks on write and we
            // block on read -> deadlock). A single background thread for
            // stderr is sufficient — we keep stdout on the calling thread so
            // no executor lifecycle is required.
            Process finalProcess = process;
            StreamCollector errCollector = new StreamCollector(finalProcess.getErrorStream());
            Thread errThread = new Thread(errCollector, "git-stderr-reader");
            errThread.setDaemon(true);
            errThread.start();

            String stdout = readAll(process.getInputStream());
            int exit = process.waitFor();
            errThread.join();
            if (errCollector.failure != null) {
                throw errCollector.failure;
            }
            return new ProcessResult(exit, stdout, errCollector.result);
        } catch (IOException e) {
            throw new UncheckedIOException("git invocation failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroy();
            }
            throw new UncheckedIOException(new IOException("git invocation interrupted", e));
        }
    }

    /**
     * Runnable that fully drains an {@link java.io.InputStream} into memory,
     * capturing any {@link IOException} for the owning thread to rethrow.
     */
    private static final class StreamCollector implements Runnable {
        private final java.io.InputStream stream;
        private volatile String result = "";
        private volatile IOException failure;

        StreamCollector(java.io.InputStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            try {
                this.result = readAll(stream);
            } catch (IOException e) {
                this.failure = e;
            }
        }
    }

    private static String readAll(java.io.InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while ((read = reader.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }
            return sb.toString();
        }
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) { }
}
