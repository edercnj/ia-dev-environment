package dev.iadev.parallelism;

import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Pairwise collision classifier for two
 * {@link FileFootprint} instances (RULE-003).
 *
 * <p>Given two units A and B, returns at most one
 * {@link Collision} describing the strongest category of
 * overlap between them. Priority ordering:</p>
 *
 * <ol>
 *   <li><b>Hotspot override</b> — if both units touch
 *       (write or regen) any path matching a
 *       {@link HotspotCatalog} pattern, the collision is
 *       classified as {@link CollisionCategory#HARD} with
 *       the hotspot path in {@code reason} (RULE-004).</li>
 *   <li><b>Hard</b> — non-empty {@code A.write ∩ B.write}.</li>
 *   <li><b>Regen</b> — non-empty
 *       {@code A.write ∩ B.regen}, {@code A.regen ∩ B.write},
 *       or {@code A.regen ∩ B.regen}.</li>
 *   <li><b>Soft</b> — read-only overlap; returned only when
 *       explicitly requested (callers may filter them out).
 *   </li>
 * </ol>
 *
 * <p>The returned {@link Collision#a()} / {@link Collision#b()}
 * pair is always sorted lexicographically so that
 * {@code detect("x", "y")} and {@code detect("y", "x")}
 * produce byte-identical output (RULE-008).</p>
 */
public final class CollisionDetector {

    private final HotspotCatalog catalog;

    public CollisionDetector() {
        this(new HotspotCatalog());
    }

    public CollisionDetector(HotspotCatalog catalog) {
        this.catalog = Objects.requireNonNull(
                catalog, "catalog");
    }

    /**
     * Classifies the pairwise overlap between two units.
     *
     * @param idA        identifier of the first unit
     * @param fpA        footprint of the first unit
     * @param idB        identifier of the second unit
     * @param fpB        footprint of the second unit
     * @param includeSoft if {@code true}, soft (read-only)
     *                   overlaps are returned;
     *                   if {@code false}, they are filtered.
     * @return the strongest collision detected, or empty
     *         when units are independent
     */
    public Optional<Collision> detect(
            String idA, FileFootprint fpA,
            String idB, FileFootprint fpB,
            boolean includeSoft) {
        Objects.requireNonNull(idA, "idA");
        Objects.requireNonNull(idB, "idB");
        Objects.requireNonNull(fpA, "fpA");
        Objects.requireNonNull(fpB, "fpB");
        if (idA.equals(idB)) {
            return Optional.empty();
        }
        String left = idA.compareTo(idB) <= 0 ? idA : idB;
        String right = left.equals(idA) ? idB : idA;
        FileFootprint lfp = left.equals(idA) ? fpA : fpB;
        FileFootprint rfp = left.equals(idA) ? fpB : fpA;

        Optional<Collision> hotspot =
                detectHotspot(left, right, lfp, rfp);
        if (hotspot.isPresent()) {
            return hotspot;
        }

        TreeSet<String> hardShared = intersect(
                lfp.writes(), rfp.writes());
        if (!hardShared.isEmpty()) {
            return Optional.of(new Collision(
                    left, right, CollisionCategory.HARD,
                    hardShared, null));
        }

        TreeSet<String> regenShared = new TreeSet<>();
        regenShared.addAll(intersect(
                lfp.writes(), rfp.regens()));
        regenShared.addAll(intersect(
                lfp.regens(), rfp.writes()));
        regenShared.addAll(intersect(
                lfp.regens(), rfp.regens()));
        if (!regenShared.isEmpty()) {
            return Optional.of(new Collision(
                    left, right, CollisionCategory.REGEN,
                    regenShared, null));
        }

        if (includeSoft) {
            TreeSet<String> softShared = intersect(
                    lfp.reads(), rfp.reads());
            if (!softShared.isEmpty()) {
                return Optional.of(new Collision(
                        left, right,
                        CollisionCategory.SOFT,
                        softShared, null));
            }
        }

        return Optional.empty();
    }

    private Optional<Collision> detectHotspot(
            String left, String right,
            FileFootprint lfp, FileFootprint rfp) {
        TreeSet<String> lTouches = new TreeSet<>();
        lTouches.addAll(lfp.writes());
        lTouches.addAll(lfp.regens());
        TreeSet<String> rTouches = new TreeSet<>();
        rTouches.addAll(rfp.writes());
        rTouches.addAll(rfp.regens());

        for (String lp : lTouches) {
            String lhit = catalog.matchHotspot(lp);
            if (lhit == null) {
                continue;
            }
            for (String rp : rTouches) {
                String rhit = catalog.matchHotspot(rp);
                if (rhit != null && rhit.equals(lhit)) {
                    return Optional.of(new Collision(
                            left, right,
                            CollisionCategory.HARD,
                            new TreeSet<>(java.util.List.of(
                                    lp, rp)),
                            "hotspot: " + lhit));
                }
            }
        }
        return Optional.empty();
    }

    private static TreeSet<String> intersect(
            java.util.Set<String> a,
            java.util.Set<String> b) {
        TreeSet<String> out = new TreeSet<>(a);
        out.retainAll(b);
        return out;
    }
}
