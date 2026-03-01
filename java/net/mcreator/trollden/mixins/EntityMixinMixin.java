package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.mcreator.trollden.util.TrolldenEntityAABBTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into Entity to:
 *
 *  1. Block Deathlist's setBoundingBox(ZERO, ZERO) corruption attack on TrolldenEntity.
 *     Deathlist.killEntity() does:
 *         entity.setBoundingBox(new AABB(Vec3.ZERO, Vec3.ZERO));
 *     We simply ignore that call when the entity is a TrolldenEntity and
 *     the caller is not our own mod.
 *
 *  2. Keep TrolldenEntityAABBTracker up-to-date after every legitimate
 *     setBoundingBox call so the AABBMixin always has the current reference.
 */
@Mixin(Entity.class)
public abstract class EntityMixinMixin {

    /**
     * Fires before every setBoundingBox call.
     * – If the entity is TrolldenEntity and the caller is foreign: cancel entirely.
     * – Otherwise: let it through, then register the new AABB in the tracker.
     */
    @Inject(
        method = "m_20011_",   // setBoundingBox / setAABB (MCP m_20011_)
        at = @At("HEAD"),
        cancellable = true
    )
    private void trollden_onSetBoundingBox(AABB aabb, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // Only guard TrolldenEntity instances.
        if (!(self instanceof TrolldenEntity)) {
            return;
        }

        // Block if caller is foreign.
        if (isCallerForeign()) {
            ci.cancel();
            return;
        }

        // Legitimate call from our own mod – register the new AABB.
        TrolldenEntityAABBTracker.register(aabb);
    }

    // -----------------------------------------------------------------------
    // Also re-register on every tick so the tracker always has the live AABB.
    // Injects into the tail of aiStep (tick) for TrolldenEntity only.
    // m_8024_ = aiStep
    // -----------------------------------------------------------------------
    @Inject(method = "m_8024_", at = @At("TAIL"))
    private void trollden_onAiStepTail(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof TrolldenEntity) {
            TrolldenEntityAABBTracker.register(self.getBoundingBox());
        }
    }

    // -----------------------------------------------------------------------
    // Reuse the same isCallerForeign logic as in AABBMixin.
    // -----------------------------------------------------------------------
    private static boolean isCallerForeign() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();

            if (cls.startsWith("java.")
                    || cls.startsWith("sun.")
                    || cls.startsWith("org.spongepowered.asm.")
                    || cls.equals(EntityMixinMixin.class.getName())) {
                continue;
            }

            if (cls.startsWith("net.mcreator.trollden.")) {
                return false;
            }

            if (cls.startsWith("net.mcreator.ultimateskeletons.")
                    || cls.startsWith("net.mcreator.")) {
                return true;
            }

            if (cls.startsWith("net.minecraft.")
                    || cls.startsWith("net.minecraftforge.")) {
                continue;
            }

            return true;
        }
        return false;
    }
}
