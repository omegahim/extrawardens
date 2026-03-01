package net.mcreator.trollden.immortal;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.mcreator.trollden.init.TrolldenModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;

@Mod.EventBusSubscriber(modid = "trollden")
public class ImmortalTracker {

    private static WeakReference<TrolldenEntity> trackedTrollden = new WeakReference<>(null);

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof TrolldenEntity trollden) {
            trackedTrollden = new WeakReference<>(trollden);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof TrolldenEntity) {
            trackedTrollden = new WeakReference<>(null);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        TrolldenEntity trollden = trackedTrollden.get();

        if (trollden != null && !trollden.isRemoved()) {
            if (trollden.level() instanceof ServerLevel serverLevel) {
                try {
                    serverLevel.getChunkSource().addEntity(trollden);
                } catch (IllegalStateException ignored) {
                    // Already tracked — normal case
                }
            }
            return;
        }

        // Reference gone or entity removed — spawn fresh in overworld
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                TrolldenEntity fresh = TrolldenModEntities.TROLLDEN.get().create(level);
                if (fresh == null) return;
                fresh.moveTo(
                    level.getSharedSpawnPos().getX(),
                    level.getSharedSpawnPos().getY(),
                    level.getSharedSpawnPos().getZ()
                );
                level.addFreshEntity(fresh);
                return;
            }
        }
    }
}