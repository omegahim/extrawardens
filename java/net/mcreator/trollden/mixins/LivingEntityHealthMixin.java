package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.mcreator.trollden.TrolldenRespawnState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LAYER 2 — Health protection.
 *
 * Cancels setHealth(0 or NaN) on the CURRENTLY TRACKED Trollden only.
 *
 * Critically, this checks UUID against TrolldenRespawnState.trackedUUID.
 * Once the respawn handler spawns a new Trollden and updates trackedUUID to
 * point at it, the old zombie entity loses protection here. The handler can
 * then call zombie.discard() and it will go through vanilla removal cleanly,
 * sending a proper ClientboundRemoveEntitiesPacket to all clients.
 *
 * NOTE: normalattack() also writes health=0 directly via SynchedEntityData:
 *   f_19804_.m_135381_(LivingEntity.f_20961_, 0.0F)
 * This bypasses this mixin entirely. The tick handler detects that state via
 * zombie detection (NaN coords, invisible flag, etc.) and handles it there.
 */
@Mixin(LivingEntity.class)
public class LivingEntityHealthMixin {

    @Inject(
        method = "m_21153_",   // setHealth(float) — SRG name
        at = @At("HEAD"),
        cancellable = true
    )
    private void trollden_onSetHealth(float health, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof TrolldenEntity trollden)) return;

        // Only protect the currently tracked entity.
        // Zombies (old UUID after respawn) are not protected — this allows
        // the respawn handler to discard them cleanly.
        if (!trollden.getUUID().equals(TrolldenRespawnState.trackedUUID)) return;

        if (health <= 0.0F || Float.isNaN(health)) {
            ci.cancel();
        }
    }
}