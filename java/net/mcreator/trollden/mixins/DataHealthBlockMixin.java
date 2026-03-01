package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SynchedEntityData.class)
public class DataHealthBlockMixin {

    // ✅ Correct Mojmap field name
    @Shadow
    private Entity entity;

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private <T> void blockHealthSet(
            EntityDataAccessor<T> key,
            T value,
            CallbackInfo ci) {

        if (!(entity instanceof TrolldenEntity)) return;

        if (key == LivingEntity.DATA_HEALTH_ID && value instanceof Float f) {
            if (f <= 0.0F) {
                ci.cancel();
            }
        }
    }
}