package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.mcreator.trollden.immortal.TrolldenImmuneLevelCallback;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ════════════════════════════════════════════════════════════════
 * EntityCallbackLockMixin
 * ════════════════════════════════════════════════════════════════
 *
 * TARGET: Entity.m_141960_(EntityInLevelCallback) — setLevelCallback()
 *
 * WHY THIS EXISTS:
 *   TrolldenImmuneLevelCallback installed in TrolldenEntity's
 *   constructor is what makes addRemoveTask skip its entire removal
 *   block. But Deathlist also calls this at the END of addRemoveTask
 *   (line 929) to reset f_146801_ to NULL_CALLBACK:
 *
 *       entity.m_141960_(EntityInLevelCallback.f_156799_);
 *
 *   If that succeeds, the next removal attempt finds NULL_CALLBACK in
 *   f_146801_ rather than our ImmuneLevelCallback — and NULL_CALLBACK
 *   IS not an instanceof PersistentEntitySectionManager$Callback either,
 *   so addRemoveTask still skips... but the next legitimate re-registration
 *   would install a real Callback, leaving us unprotected.
 *
 *   More critically: vanilla chunk-load code calls setLevelCallback to
 *   install the real PersistentEntitySectionManager$Callback when the
 *   entity is loaded into a chunk. If that runs AFTER we install our
 *   immune callback, it replaces ours with the real one — and then
 *   addRemoveTask CAN remove Trollden.
 *
 * WHAT THIS MIXIN DOES:
 *   Intercepts every call to setLevelCallback. If the entity is
 *   TrolldenEntity AND the incoming callback is NOT our
 *   TrolldenImmuneLevelCallback, cancel the set. Our immune callback
 *   stays in place forever.
 *
 *   Exception: when TrolldenImmuneLevelCallback.install() calls
 *   m_141960_() to install itself, the incoming callback IS our class,
 *   so it is allowed through.
 */
@Mixin(Entity.class)
public class EntityCallbackLockMixin {

    /**
     * m_141960_ = Entity.setLevelCallback(EntityInLevelCallback)
     */
    @Inject(method = "m_141960_", at = @At("HEAD"), cancellable = true)
    private void trollden_lockCallback(EntityInLevelCallback callback, CallbackInfo ci) {
        // Only act on TrolldenEntity
        if (!((Object) this instanceof TrolldenEntity)) return;

        // Allow our own immune callback to be set (this is the install() call)
        if (callback instanceof TrolldenImmuneLevelCallback) return;

        // Block everything else — vanilla re-registration, Deathlist's null reset,
        // any other mod trying to touch this field.
        ci.cancel();
    }
}
