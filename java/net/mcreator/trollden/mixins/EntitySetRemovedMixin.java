package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.mcreator.trollden.TrolldenRespawnState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LAYER 1 — Removal interception.
 *
 * Injects at HEAD of Entity.setRemoved(RemovalReason) — fires at line 730 of
 * normalattack(), before NaN corruption at line 813. This is our only reliable
 * window to capture a clean position.
 *
 * Only fires for the currently tracked Trollden (UUID check). Zombie entities
 * are allowed to be removed normally so discard() works cleanly on them.
 */
@Mixin(Entity.class)
public class EntitySetRemovedMixin {

    @Inject(
        method = "m_142467_",   // setRemoved(RemovalReason) — SRG name
        at = @At("HEAD")
    )
    private void trollden_onSetRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof TrolldenEntity trollden)) return;
        if (!trollden.getUUID().equals(TrolldenRespawnState.trackedUUID)) return;

        Entity entity = (Entity)(Object) this;

        double x = entity.getX(), y = entity.getY(), z = entity.getZ();
        if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z)) {
            TrolldenRespawnState.lastPosition = new Vec3(x, y, z);
        }

        if (entity.level() instanceof ServerLevel sl) {
            TrolldenRespawnState.lastLevelKey = sl.dimension().location().toString();
        }
        TrolldenRespawnState.lastEntityId = entity.getId();
        TrolldenRespawnState.zombieUUID   = entity.getUUID();
        TrolldenRespawnState.needsRespawn = true;

        System.out.println("[TrolldenMixin] setRemoved intercepted (reason=" + reason
                + ") pos=" + TrolldenRespawnState.lastPosition);
    }
}