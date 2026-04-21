package dev.iadev.application.lifecycle;

import dev.iadev.domain.lifecycle.Divergence;
import dev.iadev.domain.lifecycle.LifecycleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the apply phase of
 * {@link LifecycleReconciler} (story-0046-0006 TASK-002).
 * Exercises atomic writes via StatusFieldParser and
 * validation against LifecycleTransitionMatrix.
 */
@DisplayName("LifecycleReconciler — apply phase")
class LifecycleReconcilerApplyTest {

    private final LifecycleReconciler reconciler =
            new LifecycleReconciler();

    @Test
    @DisplayName("apply empty list is a no-op")
    void apply_empty() {
        assertThat(reconciler.apply(List.of())).isEmpty();
        assertThat(reconciler.apply(null)).isEmpty();
    }

    @Test
    @DisplayName("apply PENDENTE→EM_ANDAMENTO rewrites md")
    void apply_singleValid(@TempDir Path epic)
            throws IOException {
        Path md = epic.resolve("s.md");
        Files.writeString(md,
                "# S\n\n**Status:** Pendente\n",
                StandardCharsets.UTF_8);
        Divergence d = new Divergence("s", md,
                LifecycleStatus.PENDENTE,
                LifecycleStatus.EM_ANDAMENTO);

        List<Path> written = reconciler.apply(List.of(d));

        assertThat(written).containsExactly(md);
        assertThat(Files.readString(md))
                .contains("**Status:** Em Andamento");
    }

    @Test
    @DisplayName("apply EM_ANDAMENTO→CONCLUIDA (happy path "
            + "of TASK-006 — v2 story completion)")
    void apply_emAndamentoToConcluida(@TempDir Path epic)
            throws IOException {
        Path md = epic.resolve("s.md");
        Files.writeString(md,
                "# S\n\n**Status:** Em Andamento\n",
                StandardCharsets.UTF_8);
        Divergence d = new Divergence("s", md,
                LifecycleStatus.EM_ANDAMENTO,
                LifecycleStatus.CONCLUIDA);

        reconciler.apply(List.of(d));

        assertThat(Files.readString(md))
                .contains("**Status:** Concluída");
    }

    @Test
    @DisplayName("apply aborts on suspicious transition "
            + "(CONCLUIDA→PENDENTE) BEFORE any write")
    void apply_suspiciousTransitionAborts(
            @TempDir Path epic) throws IOException {
        Path okFile = epic.resolve("ok.md");
        Path badFile = epic.resolve("bad.md");
        Files.writeString(okFile,
                "# ok\n\n**Status:** Pendente\n",
                StandardCharsets.UTF_8);
        Files.writeString(badFile,
                "# bad\n\n**Status:** Concluída\n",
                StandardCharsets.UTF_8);
        Divergence goodOne = new Divergence("ok", okFile,
                LifecycleStatus.PENDENTE,
                LifecycleStatus.EM_ANDAMENTO);
        Divergence suspicious = new Divergence("bad",
                badFile,
                LifecycleStatus.CONCLUIDA,
                LifecycleStatus.PENDENTE); // forbidden

        assertThatThrownBy(() -> reconciler.apply(
                List.of(goodOne, suspicious)))
                .isInstanceOf(
                        StatusTransitionInvalidException
                                .class);

        // Atomicity: ok.md must NOT have been touched
        // because validation runs before any write.
        assertThat(Files.readString(okFile))
                .contains("**Status:** Pendente");
        assertThat(Files.readString(badFile))
                .contains("**Status:** Concluída");
    }

    @Test
    @DisplayName("apply with null 'from' re-establishes the "
            + "Status header (valid by contract)")
    void apply_nullFromReestablishesHeader(
            @TempDir Path epic) throws IOException {
        Path md = epic.resolve("s.md");
        Files.writeString(md, "# header-less file\n",
                StandardCharsets.UTF_8);
        Divergence d = new Divergence("s", md, null,
                LifecycleStatus.CONCLUIDA);

        reconciler.apply(List.of(d));

        assertThat(Files.readString(md))
                .startsWith("**Status:** Concluída\n");
    }

    @Test
    @DisplayName("apply rewrites multiple files in one pass")
    void apply_multipleFiles(@TempDir Path epic)
            throws IOException {
        Path a = epic.resolve("a.md");
        Path b = epic.resolve("b.md");
        Files.writeString(a,
                "**Status:** Pendente\n",
                StandardCharsets.UTF_8);
        Files.writeString(b,
                "**Status:** Em Andamento\n",
                StandardCharsets.UTF_8);
        Divergence da = new Divergence("a", a,
                LifecycleStatus.PENDENTE,
                LifecycleStatus.EM_ANDAMENTO);
        Divergence db = new Divergence("b", b,
                LifecycleStatus.EM_ANDAMENTO,
                LifecycleStatus.CONCLUIDA);

        List<Path> written =
                reconciler.apply(List.of(da, db));

        assertThat(written).containsExactly(a, b);
        assertThat(Files.readString(a))
                .contains("**Status:** Em Andamento");
        assertThat(Files.readString(b))
                .contains("**Status:** Concluída");
    }
}
