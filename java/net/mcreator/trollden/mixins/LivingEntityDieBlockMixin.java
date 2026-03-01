package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks LivingEntity.die() from executing on TrolldenEntity.
 *
 * WHY:
 *   Deathlist calls living.die() directly.
 *   That sets:
 *     - dead = true
 *     - deathTime
 *     - triggers tickDeath()
 *     - forces death animation state
 *
 *   By cancelling at method HEAD, none of that ever runs.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDieBlockMixin {

    /**
     * die(DamageSource source)
     * Mojmap name in 1.20.1 is "die"
     */
    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void trollden_blockDie(DamageSource source, CallbackInfo ci) {

        // Only block for TrolldenEntity
        if ((Object) this instanceof TrolldenEntity) {
            ci.cancel();
        }
    }
}