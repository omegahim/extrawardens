package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.mcreator.trollden.TrolldenRespawnState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LAYER 3 — Coordinate protection.
 *
 * Cancels moveTo(NaN, NaN, NaN) on the currently tracked Trollden.
 *
 * normalattack() ALSO writes NaN directly to the raw position fields (f_19791_
 * etc.) after calling moveTo — those field writes cannot be intercepted by any
 * mixin. The tick handler detects NaN coordinates as a zombie state and triggers
 * a clean replacement spawn.
 */
@Mixin(Entity.class)
public class EntityPositionMixin {

    @Inject(
        method = "m_20343_",   // moveTo(double, double, double) — SRG name
        at = @At("HEAD"),
        cancellable = true
    )
    private void trollden_onMoveTo(double x, double y, double z, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof TrolldenEntity trollden)) return;
        if (!trollden.getUUID().equals(TrolldenRespawnState.trackedUUID)) return;

        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) ci.cancel();
    }
}