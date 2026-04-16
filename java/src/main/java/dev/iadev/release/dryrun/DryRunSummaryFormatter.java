package dev.iadev.release.dryrun;

import java.util.Objects;

/**
 * Renders a human-readable summary of a
 * {@link DryRunSummary} matching story-0039-0013 §5.2.
 */
public final class DryRunSummaryFormatter {

    private DryRunSummaryFormatter() {
        throw new AssertionError("no instances");
    }

    /**
     * Formats a dry-run summary block.
     *
     * @param summary simulation summary
     * @return multi-line formatted output
     */
    public static String format(DryRunSummary summary) {
        Objects.requireNonNull(summary, "summary");
        StringBuilder sb = new StringBuilder();
        sb.append("=== DRY-RUN SUMMARY ===\n");
        sb.append(String.format(
                "Versao simulada:    %s%n",
                summary.version()));
        sb.append(String.format(
                "Fases simuladas:    %d / %d%n",
                summary.simulatedCount(),
                summary.totalPhases()));
        sb.append(String.format(
                "Fases puladas:      %d%n",
                summary.skippedCount()));
        if (summary.aborted()) {
            sb.append(String.format(
                    "Fases nao alcancadas: %d (aborted)%n",
                    summary.notReachedCount()));
        }
        sb.append(String.format(
                "Comandos previstos: %d (nenhum executado)%n",
                summary.predictedCommands()));
        sb.append("\nMODO DRY-RUN — "
                + "nenhum efeito colateral foi aplicado.\n");
        sb.append("State dummy descartado.\n");
        return sb.toString();
    }
}
