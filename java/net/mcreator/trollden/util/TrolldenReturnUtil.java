package net.mcreator.trollden.util;

/**
 * Controls whether Trollden is stripped from ALL entity query results,
 * or only from foreign mod queries (via stack inspection).
 *
 * allReturn = true  → nuclear mode. Trollden invisible to everyone except
 *                     code explicitly calling your own methods. Zero stack
 *                     walk overhead. Use this if you never need other mods
 *                     to find Trollden at all.
 *
 * allReturn = false → selective mode. Stack inspection runs on each query.
 *                     Trollden is still visible to net.mcreator.trollden,
 *                     net.minecraft, net.minecraftforge, and com.mojang.
 *                     All other mods (StarlightSkeleton, Deathlist, etc.)
 *                     get an empty result.
 *
 * Default: true (nuclear). Toggle with setAllReturn() at any time.
 */
public class TrolldenReturnUtil {

    private static volatile boolean allReturn = true;

    /**
     * Returns true if nuclear mode is on — strip Trollden from all queries.
     * Called every time Level.getEntities / getEntitiesOfClass is invoked.
     */
    public static boolean isAllReturn() {
        return allReturn;
    }

    /**
     * Switch modes at runtime if needed.
     * e.g. TrolldenReturnUtil.setAllReturn(false) to let vanilla find Trollden again.
     */
    public static void setAllReturn(boolean value) {
        allReturn = value;
    }
}