package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks client-side entity lifecycle removal for TrolldenEntity.
 *
 * WHY THIS FIXES THE JITTER/DISAPPEAR:
 *   When Starlightskeleton's attack fires, the server sends a
 *   ClientboundRemoveEntitiesPacket to all clients. The client
 *   processes this through ClientLevel$EntityCallbacks:
 *
 *     onDestroyed()    → removes Trollden from ClientLevel's entity list
 *                        → EntityRenderer loses its reference → invisible
 *     onTrackingEnd()  → entity tracker releases Trollden
 *                        → stops receiving position updates from server
 *
 *   Even though the server entity survives (field repair working),
 *   the client has already deleted it. The "correct angle" visibility
 *   is Trollden still being in the client's chunk section storage
 *   (EntitySection) but NOT in the active entity list — so it only
 *   appears when the frustum queries the section directly.
 *   The jitter is conflicting server position packets arriving for
 *   an entity the client renderer thinks is in a death state.
 *
 *   Blocking onDestroyed and onTrackingEnd here keeps Trollden in
 *   the client's entity list and renderer permanently, exactly
 *   mirroring what ChaosWither's MixinClientLevelEntityCallbacks does.
 *
 * NOTE: This MUST be in the "client" array in mixins.trollden.json,
 *       NOT the "mixins" array. ClientLevel only exists on the client.
 */
@Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel$EntityCallbacks")
public class ClientEntityLevelCallbacksMixin {

    @Inject(method = "onDestroyed", at = @At("HEAD"), cancellable = true)
    private void trollden_blockDestroyed(Entity entity, CallbackInfo ci) {
        if (entity instanceof TrolldenEntity) {
            ci.cancel();
        }
    }

    @Inject(method = "onTrackingEnd", at = @At("HEAD"), cancellable = true)
    private void trollden_blockTrackingEnd(Entity entity, CallbackInfo ci) {
        if (entity instanceof TrolldenEntity) {
            ci.cancel();
        }
    }
}

