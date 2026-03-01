package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks DYING pose being written directly into SynchedEntityData for TrolldenEntity.
 *
 * WHY THIS IS NEEDED:
 *   Deathlist.die() does NOT call entity.setPose(Pose.DYING).
 *   Instead it writes directly into the SynchedEntityData system:
 *
 *       living.f_19804_.m_276349_(Entity.f_19806_, Pose.DYING, false);
 *
 *   That is SynchedEntityData.set(DATA_POSE, Pose.DYING, false).
 *   Because it bypasses setPose() entirely, overriding setPose() in
 *   TrolldenEntity does nothing against this attack.
 *
 *   This mixin intercepts every SynchedEntityData.set() call at the
 *   source and cancels it when the owner is TrolldenEntity and the
 *   incoming value is Pose.DYING.
 *
 *   Add "SynchedEntityDataPoseMixin" to the "mixins" array (not "client")
 *   in mixins.trollden.json.
 */
@Mixin(SynchedEntityData.class)
public class SynchedEntityDataPoseMixin {

    // f_135036_ = SynchedEntityData.entity (the owner entity)
    @Shadow
    private Entity entity;

    /**
     * m_276349_ = SynchedEntityData.set(EntityDataAccessor, value, force)
     * This is the three-argument variant Deathlist uses (force = false).
     */
    @Inject(method = "m_276349_", at = @At("HEAD"), cancellable = true)
    private <T> void trollden_blockDyingPose(
            EntityDataAccessor<T> key,
            T value,
            boolean force,
            CallbackInfo ci) {

        if (!(entity instanceof TrolldenEntity)) return;
        if (key.equals(Entity.DATA_POSE) && value == Pose.DYING) {
            ci.cancel();
        }
    }

    /**
     * Also block the two-argument variant m_135381_ just in case any
     * other code path uses SynchedEntityData.set(accessor, value)
     * without the force parameter.
     *
     * m_135381_ = SynchedEntityData.set(EntityDataAccessor, value)
     */
    @Inject(method = "m_135381_", at = @At("HEAD"), cancellable = true)
    private <T> void trollden_blockDyingPose2(
            EntityDataAccessor<T> key,
            T value,
            CallbackInfo ci) {

        if (!(entity instanceof TrolldenEntity)) return;
        if (key.equals(Entity.DATA_POSE) && value == Pose.DYING) {
            ci.cancel();
        }
    }
}
