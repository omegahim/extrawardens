package net.mcreator.trollden.util;

import net.minecraft.world.phys.AABB;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Tracks which AABB instances currently belong to a TrolldenEntity.
 *
 * Uses a WeakHashMap so entries are automatically cleaned up when the AABB
 * is no longer referenced anywhere (i.e. after the entity recreates its box).
 *
 * Call TrolldenEntityAABBTracker.register(this.getBoundingBox()) from
 * TrolldenEntity.tick() (or aiStep) every tick to keep it current.
 */
public final class TrolldenEntityAABBTracker {

    // WeakHashMap<AABB, ?> – the AABB is the key so GC can reclaim it freely.
    private static final Set<AABB> TRACKED = Collections.newSetFromMap(new WeakHashMap<>());

    private TrolldenEntityAABBTracker() {}

    /** Register an AABB as belonging to a TrolldenEntity. */
    public static synchronized void register(AABB aabb) {
        if (aabb != null) {
            TRACKED.add(aabb);
        }
    }

    /** Returns true if this AABB belongs to a TrolldenEntity. */
    public static synchronized boolean isTracked(AABB aabb) {
        return TRACKED.contains(aabb);
    }
}
