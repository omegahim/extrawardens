package net.mcreator.trollden.mixins;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.Entity;

/**
 * Blocks zero-size or invalid bounding boxes being set on TrolldenEntity.
 *
 * WHY THIS FIXES THE ANGLE-DEPENDENT VISIBILITY:
 *   Deathlist calls m_20011_(new AABB(Vec3.ZERO, Vec3.ZERO)) which sets
 *   Trollden's bounding box to a single point at the world origin (0,0,0).
 *   Minecraft's frustum culler checks whether the AABB intersects the
 *   camera frustum to decide whether to render an entity.
 *   A zero-size AABB at the wrong position means the entity only "appears"
 *   from the exact camera angle where that zero-point sits in the frustum —
 *   which is exactly the specific angle the user discovered.
 *
 *   This fires on BOTH sides — server (Deathlist runs server-side) and
 *   client (the zero AABB is replicated via entity tracking packets).
 *   Put this in the "mixins" array (not "client") so it applies both sides.
 */
@Mixin(Entity.class)
public class EntityBoundingBoxMixin {

    /**
     * m_20011_ = Entity.setBoundingBox(AABB)
     * Cancel if the incoming AABB is zero-size or nonsensically small,
     * which is never a valid state for a living entity.
     */
    @Inject(method = "m_20011_", at = @At("HEAD"), cancellable = true)
    private void trollden_blockZeroAABB(AABB aabb, CallbackInfo ci) {
        if (!((Object) this instanceof TrolldenEntity)) return;

        // An AABB is "zero" or invalid if its volume is effectively nothing.
        // Legitimate bounding boxes for any mob are always at least a few
        // tenths of a block in each dimension.
        double sizeX = aabb.maxX - aabb.minX;
        double sizeY = aabb.maxY - aabb.minY;
        double sizeZ = aabb.maxZ - aabb.minZ;

        if (sizeX < 0.01 || sizeY < 0.01 || sizeZ < 0.01) {
            ci.cancel();
        }
    }
}
