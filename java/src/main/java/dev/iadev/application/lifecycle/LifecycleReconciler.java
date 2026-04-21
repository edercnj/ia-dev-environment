package dev.iadev.application.lifecycle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iadev.domain.lifecycle.Divergence;
import dev.iadev.domain.lifecycle.LifecycleStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reconciles {@code execution-state.json} (telemetry) against
 * the canonical {@code **Status:**} fields of Epic / Story
 * markdown artifacts for a single epic directory.
 *
 * <p>This is the diff phase (story-0046-0006 TASK-001). The
 * apply phase is added by TASK-002 on top of the stable diff
 * contract exposed here.</p>
 *
 * <p>Rule 19 — legacy v1 epics (no {@code version} field or
 * value {@code "1.0"}) are skipped: {@link #isLegacyV1(Path)}
 * returns true and callers emit the "skipping" message. Rule
 * 22 — this class reads markdown via
 * {@link StatusFieldParser}; it NEVER writes
 * {@code execution-state.json} (RULE-046-07).</p>
 */
public final class LifecycleReconciler {

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    private static final String EXEC_STATE_FILE =
            "execution-state.json";
    private static final String IMPL_MAP_FILE =
            "IMPLEMENTATION-MAP.md";

    /**
     * Returns {@code true} when the epic directory has no
     * {@code execution-state.json} OR the state file's
     * {@code version} field is absent / equals {@code "1.0"}.
     * Rule 19 — legacy epics skip sync without error.
     *
     * @throws StatusSyncException when the file exists but
     *     cannot be parsed (malformed JSON or unreadable).
     */
    public boolean isLegacyV1(Path epicDir) {
        Path state = resolveStateFile(epicDir);
        if (!Files.exists(state)) {
            return true;
        }
        JsonNode root = readJsonOrThrow(state);
        JsonNode v = root.get("version");
        if (v == null || v.isNull()) {
            return true;
        }
        return "1.0".equals(v.asText());
    }

    /**
     * Computes the divergences for the given epic directory.
     *
     * <p>The returned list contains one {@link Divergence} per
     * (artifact, mismatch) pair. A mismatch is defined as:
     * the markdown's {@code **Status:**} value (per
     * {@link StatusFieldParser}) differs from the status
     * derived from {@code execution-state.json} via
     * {@link #mapCheckpointToStatus(String)}.</p>
     *
     * <p>Rule 19 — legacy v1 epics produce an empty list and
     * callers print the "skipping" message.</p>
     *
     * @throws StatusSyncException when
     *     {@code execution-state.json} is missing (not legacy)
     *     or malformed — signals exit code 30 to the CLI.
     */
    public List<Divergence> diff(Path epicDir) {
        if (epicDir == null) {
            throw new StatusSyncException(null,
                    "epicDir is null");
        }
        Path state = resolveStateFile(epicDir);
        if (!Files.exists(state)) {
            throw new StatusSyncException(state,
                    "execution-state.json not found");
        }
        JsonNode root = readJsonOrThrow(state);
        if (isLegacyV1(epicDir)) {
            return Collections.emptyList();
        }
        List<Divergence> out = new ArrayList<>();
        JsonNode stories = root.get("stories");
        if (stories != null && stories.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it =
                    stories.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                collectStoryDivergence(epicDir,
                        e.getKey(), e.getValue(), out);
            }
        }
        collectEpicDivergence(epicDir, root, out);
        return out;
    }

    private void collectStoryDivergence(Path epicDir,
            String storyId, JsonNode storyNode,
            List<Divergence> out) {
        if (storyNode == null || !storyNode.isObject()) {
            return;
        }
        JsonNode statusNode = storyNode.get("status");
        if (statusNode == null || statusNode.isNull()) {
            return;
        }
        Optional<LifecycleStatus> target =
                mapCheckpointToStatus(statusNode.asText());
        if (target.isEmpty()) {
            return;
        }
        Path md = epicDir.resolve(storyId + ".md");
        if (!Files.exists(md)) {
            return;
        }
        Optional<LifecycleStatus> current =
                StatusFieldParser.readStatus(md);
        LifecycleStatus from = current.orElse(null);
        if (from == target.get()) {
            return;
        }
        out.add(new Divergence(storyId, md, from,
                target.get()));
    }

    private void collectEpicDivergence(Path epicDir,
            JsonNode root, List<Divergence> out) {
        String epicId = root.path("epicId").asText(null);
        if (epicId == null || epicId.isBlank()) {
            return;
        }
        // Epic is CONCLUIDA iff every story resolves to
        // CONCLUIDA. Short-circuit on the first non-concluida.
        JsonNode stories = root.get("stories");
        if (stories == null || !stories.isObject()
                || stories.size() == 0) {
            return;
        }
        boolean allDone = true;
        Iterator<JsonNode> it = stories.elements();
        while (it.hasNext()) {
            JsonNode s = it.next();
            Optional<LifecycleStatus> st =
                    mapCheckpointToStatus(
                            s.path("status").asText(""));
            if (st.isEmpty()
                    || st.get() != LifecycleStatus.CONCLUIDA) {
                allDone = false;
                break;
            }
        }
        if (!allDone) {
            return;
        }
        Path epicMd = epicDir.resolve(
                "epic-" + epicId + ".md");
        if (!Files.exists(epicMd)) {
            return;
        }
        Optional<LifecycleStatus> current =
                StatusFieldParser.readStatus(epicMd);
        LifecycleStatus from = current.orElse(null);
        if (from == LifecycleStatus.CONCLUIDA) {
            return;
        }
        out.add(new Divergence("epic-" + epicId, epicMd,
                from, LifecycleStatus.CONCLUIDA));
    }

    /**
     * Maps a {@code execution-state.json} status token to the
     * canonical markdown {@link LifecycleStatus}.
     *
     * <p>Mapping (story-0046-0006 §3 "Data Contract"):</p>
     * <ul>
     *   <li>{@code SUCCESS}, {@code MERGED}, {@code COMPLETE}
     *       → {@code Concluída}</li>
     *   <li>{@code IN_PROGRESS} → {@code Em Andamento}</li>
     *   <li>{@code PENDING} → {@code Pendente}</li>
     *   <li>{@code FAILED} → {@code Falha}</li>
     *   <li>{@code BLOCKED} → {@code Bloqueada}</li>
     *   <li>{@code PLANNED} → {@code Planejada}</li>
     * </ul>
     *
     * Unknown tokens return {@link Optional#empty()} — caller
     * skips the artifact (not a divergence).
     */
    public static Optional<LifecycleStatus>
            mapCheckpointToStatus(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return switch (token) {
            case "SUCCESS", "MERGED", "COMPLETE", "DONE" ->
                    Optional.of(LifecycleStatus.CONCLUIDA);
            case "IN_PROGRESS" ->
                    Optional.of(LifecycleStatus.EM_ANDAMENTO);
            case "PENDING" ->
                    Optional.of(LifecycleStatus.PENDENTE);
            case "FAILED" ->
                    Optional.of(LifecycleStatus.FALHA);
            case "BLOCKED" ->
                    Optional.of(LifecycleStatus.BLOQUEADA);
            case "PLANNED" ->
                    Optional.of(LifecycleStatus.PLANEJADA);
            default -> Optional.empty();
        };
    }

    /**
     * Applies the given divergences to disk via
     * {@link StatusFieldParser#writeStatus(Path,
     * LifecycleStatus)}.
     *
     * <p>Every divergence is pre-validated for regression: a
     * target whose lifecycle rank is strictly lower than the
     * source's rank is a semantic regression (e.g. markdown=
     * Concluída but state.json=PENDING) and aborts the apply
     * BEFORE any write, throwing
     * {@link StatusTransitionInvalidException} (exit 40, per
     * story §3.3). Forward backfill transitions (e.g.
     * PENDENTE → CONCLUIDA) are ALWAYS allowed — that is
     * precisely the legacy-epic recovery the skill exists to
     * support.</p>
     *
     * <p>Null {@code from} (markdown has no Status header) is
     * special-cased as ALLOWED for any target — Rule 22
     * permits re-establishing the invariant by prepending the
     * Status line. This matches
     * {@link StatusFieldParser#writeStatus} behaviour.</p>
     *
     * <p>Rule 19 — callers must check
     * {@link #isLegacyV1(Path)} FIRST and short-circuit; this
     * method does NOT inspect the epic directory.</p>
     *
     * <p>RULE-046-07 — never writes to
     * {@code execution-state.json}. The markdown is the
     * single source of truth; the state file is telemetry.</p>
     *
     * @param divergences the divergences to apply (no-op when
     *     empty)
     * @return the list of files actually rewritten (each
     *     appears at most once — {@code IMPLEMENTATION-MAP.md}
     *     would be deduplicated if multiple row-level
     *     divergences targeted it; currently per-story
     *     divergences do not touch the map)
     */
    public List<Path> apply(List<Divergence> divergences) {
        if (divergences == null || divergences.isEmpty()) {
            return Collections.emptyList();
        }
        // Pre-validate every transition BEFORE any write.
        for (Divergence d : divergences) {
            validateTransition(d);
        }
        List<Path> written = new ArrayList<>();
        for (Divergence d : divergences) {
            StatusFieldParser.writeStatus(d.file(), d.to());
            if (!written.contains(d.file())) {
                written.add(d.file());
            }
        }
        return written;
    }

    private static void validateTransition(Divergence d) {
        if (d.from() == null) {
            // Re-establishing the invariant is always valid.
            return;
        }
        // Reconciliation is a retroactive sync: markdown is
        // catching up to state.json (telemetry). Forward
        // backfill (e.g. PENDENTE → CONCLUIDA, Em Andamento →
        // Concluída) is ALWAYS allowed because it restores the
        // Rule 22 invariant that got out of sync. The matrix
        // is consulted only to detect the suspicious case —
        // markdown CONCLUIDA with state.json=PENDING — which
        // is semantically a regression and must abort loudly
        // (story §3.3). The rank ordering below codifies the
        // forward lifecycle direction; a target whose rank is
        // lower than the source's rank is a regression.
        if (rank(d.to()) < rank(d.from())) {
            throw new StatusTransitionInvalidException(
                    d.from(), d.to());
        }
    }

    private static int rank(LifecycleStatus s) {
        return switch (s) {
            case PENDENTE -> 0;
            case PLANEJADA -> 1;
            case EM_ANDAMENTO -> 2;
            case BLOQUEADA -> 3;
            case FALHA -> 4;
            case CONCLUIDA -> 5;
        };
    }

    static Path resolveStateFile(Path epicDir) {
        return epicDir.resolve(EXEC_STATE_FILE);
    }

    static Path resolveImplMap(Path epicDir) {
        return epicDir.resolve(IMPL_MAP_FILE);
    }

    private static JsonNode readJsonOrThrow(Path state) {
        try {
            byte[] bytes = Files.readAllBytes(state);
            if (bytes.length == 0) {
                throw new StatusSyncException(state,
                        "execution-state.json is empty");
            }
            return MAPPER.readTree(new String(bytes,
                    StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new StatusSyncException(state,
                    "failed to read/parse "
                            + "execution-state.json", e);
        }
    }
}
