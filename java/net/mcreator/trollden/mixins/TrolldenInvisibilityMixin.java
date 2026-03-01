package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerLevel.class, remap = false)
public class TrolldenInvisibilityMixin {

    /**
     * Deathlist.getAllEntities() calls serverLevel.m_8583_() (ServerLevel.getAllEntities())
     * and iterates the result directly — completely bypassing Level.getEntities() and AABB.
     * We wrap the returned Iterable to silently skip any TrolldenEntity.
     */
    @Inject(
        method = "m_8583_()Ljava/lang/Iterable;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void filterAllEntities(CallbackInfoReturnable<Iterable<Entity>> cir) {
        Iterable<Entity> original = cir.getReturnValue();
        if (original == null) return;
        cir.setReturnValue(() -> {
            java.util.Iterator<Entity> inner = original.iterator();
            return new java.util.Iterator<Entity>() {
                private Entity next = null;
                private boolean hasNext = false;

                private void advance() {
                    while (inner.hasNext()) {
                        Entity candidate = inner.next();
                        if (!(candidate instanceof TrolldenEntity)) {
                            next = candidate;
                            hasNext = true;
                            return;
                        }
                    }
                    hasNext = false;
                    next = null;
                }

                @Override
                public boolean hasNext() {
                    if (!hasNext) advance();
                    return hasNext;
                }

                @Override
                public Entity next() {
                    if (!hasNext) advance();
                    if (!hasNext) throw new java.util.NoSuchElementException();
                    hasNext = false;
                    return next;
                }
            };
        });
    }
}