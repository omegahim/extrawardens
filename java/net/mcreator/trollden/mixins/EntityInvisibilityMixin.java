package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.mcreator.trollden.TrolldenRespawnState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LAYER 4 — Visibility protection.
 *
 * Cancels setInvisible(true) on the currently tracked Trollden.
 * Zombies are not protected — they need to be properly removed.
 *
 * Note: normalattack() also directly writes the shared flags SynchedEntityData,
 * which can bypass this mixin. The tick handler detects that case via zombie
 * detection and handles it by spawning a replacement.
 */
@Mixin(Entity.class)
public class EntityInvisibilityMixin {

    @Inject(
        method = "m_6842_",   // setInvisible(boolean) — SRG name
        at = @At("HEAD"),
        cancellable = true
    )
    private void trollden_onSetInvisible(boolean invisible, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof TrolldenEntity trollden)) return;
        if (!trollden.getUUID().equals(TrolldenRespawnState.trackedUUID)) return;

        if (invisible) ci.cancel();
    }
}