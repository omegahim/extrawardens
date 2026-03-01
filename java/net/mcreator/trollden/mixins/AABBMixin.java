package net.mcreator.trollden.mixins;

import net.mcreator.trollden.util.TrolldenEntityAABBTracker;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into AABB to silently block any mod outside net.mcreator.trollden
 * from intersecting with a TrolldenEntity's bounding box.
 *
 * How the attack mods use AABB:
 *   – StarlightskeletonATKProcedure / angry variant / ultimate variant:
 *       world.getEntitiesOfClass(Entity.class, new AABB(center).inflate(radius), pred)
 *       → internally calls AABB#intersects(AABB) to test each entity section / entity.
 *   – Deathlist does direct manipulation (handled separately in EntityMixin).
 *
 * By returning false from intersects() when the "other" AABB is one we track
 * as belonging to TrolldenEntity and the caller is NOT from net.mcreator.trollden,
 * TrolldenEntity simply won't appear in any getEntitiesOfClass results from those mods.
 *
 * Stack-trace walking is O(n) but it only fires when a tracked AABB is actually
 * involved, keeping the hot path (no TrolldenEntity in world) essentially free.
 */
@Mixin(AABB.class)
public abstract class AABBMixin {

    // -----------------------------------------------------------------------
    // intersects(AABB other)
    // Called by entity section lookup to decide whether an entity is "in" a
    // search box.  If *other* is the TrolldenEntity's box, pretend it isn't.
    // -----------------------------------------------------------------------
    @Inject(
        method = "intersects(Lnet/minecraft/world/phys/AABB;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void trollden_onIntersects(AABB other, CallbackInfoReturnable<Boolean> cir) {
        // Fast exit: neither box is tracked → nothing to do.
        if (!TrolldenEntityAABBTracker.isTracked(other)
                && !TrolldenEntityAABBTracker.isTracked((AABB) (Object) this)) {
            return;
        }

        // One of the boxes belongs to TrolldenEntity. Check who is asking.
        if (isCallerForeign()) {
            cir.setReturnValue(false);
        }
    }

    // -----------------------------------------------------------------------
    // intersects(double minX, double minY, double minZ,
    //            double maxX, double maxY, double maxZ)
    // The six-double overload is used by some internal paths; protect it too.
    // -----------------------------------------------------------------------
    @Inject(
        method = "intersects(DDDDDD)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void trollden_onIntersects6(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            CallbackInfoReturnable<Boolean> cir) {

        // We can only guard "this" here (we have no AABB reference for the args).
        if (!TrolldenEntityAABBTracker.isTracked((AABB) (Object) this)) {
            return;
        }

        if (isCallerForeign()) {
            cir.setReturnValue(false);
        }
    }

    // -----------------------------------------------------------------------
    // clip(Vec3 from, Vec3 to)
    // Used for ray-cast / projectile hit-detection.
    // Return empty Optional so no hit registers on TrolldenEntity from outside.
    // -----------------------------------------------------------------------
    @Inject(
        method = "clip",
        at = @At("HEAD"),
        cancellable = true
    )
    private void trollden_onClip(
            net.minecraft.world.phys.Vec3 from,
            net.minecraft.world.phys.Vec3 to,
            CallbackInfoReturnable<java.util.Optional<net.minecraft.world.phys.Vec3>> cir) {

        if (!TrolldenEntityAABBTracker.isTracked((AABB) (Object) this)) {
            return;
        }

        if (isCallerForeign()) {
            cir.setReturnValue(java.util.Optional.empty());
        }
    }

    // -----------------------------------------------------------------------
    // Helper – walks the call stack looking for a frame from our own mod.
    // Returns TRUE when the deepest recognisable caller is NOT trollden
    // (i.e. it IS a foreign/enemy mod).
    // -----------------------------------------------------------------------
    private static boolean isCallerForeign() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();

            // Skip JVM / Mixin / our own mixin infrastructure frames.
            if (cls.startsWith("java.")
                    || cls.startsWith("sun.")
                    || cls.startsWith("org.spongepowered.asm.")
                    || cls.equals(AABBMixin.class.getName())) {
                continue;
            }

            // First meaningful frame that belongs to our mod → not foreign.
            if (cls.startsWith("net.mcreator.trollden.")) {
                return false;
            }

            // First meaningful frame from ultimateskeletons or any other mod → foreign.
            if (cls.startsWith("net.mcreator.ultimateskeletons.")
                    || cls.startsWith("net.mcreator.")) {  // catch any other MCreator mod too
                return true;
            }

            // Vanilla / Forge frames are fine; keep scanning upwards.
            if (cls.startsWith("net.minecraft.")
                    || cls.startsWith("net.minecraftforge.")) {
                continue;
            }

            // Unknown third-party frame → treat as foreign to be safe.
            // Remove this branch if it causes false positives with your other mods.
            return true;
        }
        // No suspicious caller found → allow.
        return false;
    }
}