package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks Trollden from being evicted from the client's chunk section storage.
 *
 * WHY THIS IS NEEDED IN ADDITION TO ClientLevelEntityCallbacksMixin:
 *   ClientLevelEntityCallbacksMixin blocks the high-level entity list removal.
 *   But one layer below that, TransientEntitySectionManager$Callback.onRemove()
 *   evicts the entity from the spatial chunk section index.
 *
 *   Without this mixin, Trollden stays in the entity list (so it renders)
 *   but disappears from spatial queries like getEntitiesOfClass() and the
 *   chunk-section-based rendering culling. This is why the visibility is
 *   angle-dependent — some render paths query the section, others query
 *   the list, and they give different answers.
 *
 *   With both mixins: Trollden is in both the entity list AND the section
 *   storage on the client, permanently, matching server state.
 *
 * NOTE: Must be in the "client" array in mixins.trollden.json.
 */
@Mixin(targets = "net.minecraft.world.level.entity.TransientEntitySectionManager$Callback")
public class TransientSectionCallbackMixin<T extends EntityAccess> {

    // The entity this specific Callback instance is associated with.
    @Shadow @Final
    T f_157668_;

    /**
     * m_142472_ = LevelCallback.onRemove(RemovalReason)
     * Called when the entity is being evicted from its chunk section.
     */
    @Inject(method = "m_142472_", at = @At("HEAD"), cancellable = true)
    private void trollden_blockSectionRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (f_157668_ instanceof TrolldenEntity) {
            ci.cancel();
        }
    }
}
