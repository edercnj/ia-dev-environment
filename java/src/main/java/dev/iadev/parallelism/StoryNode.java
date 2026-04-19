package dev.iadev.parallelism;

import java.util.List;

/**
 * Internal story-node carrier shared by {@link FootprintLoader},
 * {@link ParallelismEvaluator}, and {@link ParallelismReportBuilder}.
 *
 * <p>Package-private on purpose: callers outside the parallelism
 * package use the public {@link ParallelismEvaluator.Report} record
 * instead.</p>
 */
record StoryNode(
        String id,
        FileFootprint footprint,
        List<String> blockedBy) {
}
