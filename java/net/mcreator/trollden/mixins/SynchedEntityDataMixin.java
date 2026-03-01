package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.mcreator.trollden.TrolldenRespawnState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(SynchedEntityData.class)
public class SynchedEntityDataMixin {

    // Cached reference to the Entity-typed field inside SynchedEntityData.
    // We find it by type rather than by name so we never need to know the
    // SRG name (which varies by environment and version).
    private static Field ENTITY_FIELD = null;

    private static Field getEntityField() {
        if (ENTITY_FIELD != null) return ENTITY_FIELD;
        for (Field f : SynchedEntityData.class.getDeclaredFields()) {
            if (Entity.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                ENTITY_FIELD = f;
                return f;
            }
        }
        return null;
    }

    @Inject(
        method = "m_135381_",  // set(EntityDataAccessor<T>, T) — SRG name
        at = @At("HEAD"),
        cancellable = true
    )
    private <T> void trollden_onSet(EntityDataAccessor<T> accessor, T value, CallbackInfo ci) {
        Field entityField = getEntityField();
        if (entityField == null) return;

        Entity owner;
        try {
            owner = (Entity) entityField.get(this);
        } catch (Exception e) {
            return;
        }

        if (!(owner instanceof TrolldenEntity trollden)) return;
        if (!trollden.getUUID().equals(TrolldenRespawnState.trackedUUID)) return;

        // LivingEntity.f_20961_ is the DATA_HEALTH_ID accessor.
        // Block any write that sets health to 0 or NaN.
        if (accessor.equals(LivingEntity.DATA_HEALTH_ID)) {
            if (value instanceof Float health && (health <= 0.0F || Float.isNaN(health))) {
                ci.cancel();
            }
        }
    }
}