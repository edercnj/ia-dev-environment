package dev.iadev.cli;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.domain.model.Platform;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Formats platform filter information for verbose and
 * dry-run output modes.
 *
 * <p>Ensures dry-run output respects the selected platform
 * filter and verbose output shows both included and skipped
 * assemblers with the reason for each decision.</p>
 *
 * @see VerbosePipelineRunner
 * @see CliDisplay
 */
final class PlatformVerboseFormatter {

    private static final String ALL_LABEL = "all";

    private PlatformVerboseFormatter() {
        // utility class
    }

    /**
     * Formats the platform filter header line.
     *
     * @param platforms the requested platforms (empty = all)
     * @param filtered  the filtered assembler list
     * @param all       the complete assembler list
     * @return the formatted header line
     */
    static String formatFilterHeader(
            Set<Platform> platforms,
            List<AssemblerDescriptor> filtered,
            List<AssemblerDescriptor> all) {
        String label = buildPlatformLabel(platforms);
        int total = filtered.size();

        if (isNoFilter(platforms)) {
            return "Platform filter: %s -> %d assemblers"
                    .formatted(label, all.size())
                    + " (no filter applied)";
        }

        long sharedCount = countShared(filtered);
        long platformCount = total - sharedCount;

        return "Platform filter: %s -> %d assemblers"
                .formatted(label, total)
                + " (%d platform + %d shared)"
                        .formatted(platformCount,
                                sharedCount);
    }

    /**
     * Formats an INCLUDED line for a descriptor.
     *
     * @param desc the assembler descriptor
     * @return the formatted INCLUDED line
     */
    static String formatIncluded(
            AssemblerDescriptor desc) {
        return "  INCLUDED: %s (platform: %s)"
                .formatted(desc.name(),
                        primaryPlatformName(desc));
    }

    /**
     * Formats a SKIPPED line for a descriptor.
     *
     * @param desc the assembler descriptor
     * @return the formatted SKIPPED line
     */
    static String formatSkipped(
            AssemblerDescriptor desc) {
        return "  SKIPPED: %s (platform: %s)"
                .formatted(desc.name(),
                        primaryPlatformName(desc));
    }

    /**
     * Formats the dry-run warning with platform info.
     *
     * @param platforms      the requested platforms
     * @param assemblerCount the filtered assembler count
     * @return the formatted dry-run warning
     */
    static String formatDryRunWarning(
            Set<Platform> platforms,
            int assemblerCount) {
        String label = buildPlatformLabel(platforms);
        return AssemblerPipeline.DRY_RUN_WARNING + "."
                + " Platform: %s (%d assemblers)"
                        .formatted(label, assemblerCount);
    }

    /**
     * Computes the skipped descriptors by diffing
     * filtered from all.
     *
     * @param filtered the filtered assembler list
     * @param all      the complete assembler list
     * @return the list of skipped descriptors
     */
    static List<AssemblerDescriptor> computeSkipped(
            List<AssemblerDescriptor> filtered,
            List<AssemblerDescriptor> all) {
        Set<String> includedNames = filtered.stream()
                .map(AssemblerDescriptor::name)
                .collect(Collectors.toSet());
        return all.stream()
                .filter(d -> !includedNames.contains(
                        d.name()))
                .toList();
    }

    private static String buildPlatformLabel(
            Set<Platform> platforms) {
        if (isNoFilter(platforms)) {
            return ALL_LABEL;
        }
        return platforms.stream()
                .sorted(Comparator.comparing(
                        Platform::cliName))
                .map(Platform::cliName)
                .collect(Collectors.joining(", "));
    }

    private static boolean isNoFilter(
            Set<Platform> platforms) {
        return platforms.isEmpty()
                || platforms.containsAll(
                        Platform.allUserSelectable());
    }

    private static long countShared(
            List<AssemblerDescriptor> descriptors) {
        return descriptors.stream()
                .filter(d -> d.platforms()
                        .contains(Platform.SHARED))
                .count();
    }

    private static String primaryPlatformName(
            AssemblerDescriptor desc) {
        return desc.platforms().iterator()
                .next().cliName();
    }
}
