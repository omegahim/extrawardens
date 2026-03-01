package net.mcreator.trollden;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.trollden.entity.TrolldenEntity;
import net.mcreator.trollden.init.TrolldenModEntities;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "trollden")
public class TrolldenRespawnHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;
        if (TrolldenRespawnState.trackedUUID == null) return;

        // Post-respawn cooldown — only blocks spawning a SECOND entity, not
        // detection. We still scan for the entity every tick.
        if (TrolldenRespawnState.respawnCooldown > 0) {
            TrolldenRespawnState.respawnCooldown--;
            return;
        }

        // ── Scan all levels ──────────────────────────────────────────────────
        // We look for Trollden on three conditions:
        //
        //  HEALTHY:  alive, isAddedToWorld=true, non-NaN coords.
        //            Update the cache and return — nothing to do.
        //
        //  ZOMBIE:   entity found by UUID but isAddedToWorld=false, OR coords
        //            are NaN, OR entity is invisible.
        //            This means normalattack (or equivalent from any mod) has
        //            torn the entity out of EntitySections and written corrupt
        //            state via direct field access. The entity is still "alive"
        //            server-side but invisible/untracked on clients.
        //            → Respawn immediately.
        //
        //  MISSING:  entity not found in any level at all.
        //            setRemoved() went through, or the entity was discarded.
        //            The setRemoved override in TrolldenEntity should have
        //            captured the last clean position before this point.
        //            → Respawn immediately.
        // ─────────────────────────────────────────────────────────────────────

        for (ServerLevel level : event.getServer().getAllLevels()) {
            Entity existing = level.getEntity(TrolldenRespawnState.trackedUUID);
            if (existing == null) continue;

            double ex = existing.getX(), ey = existing.getY(), ez = existing.getZ();
            boolean coordsNaN = Double.isNaN(ex) || Double.isNaN(ey) || Double.isNaN(ez);

            // HEALTHY — update cache and return.
            if (!existing.isRemoved()
                    && existing.isAlive()
                    && !coordsNaN
                    && existing.isAddedToWorld
                    && !existing.isInvisible()) {

                TrolldenRespawnState.lastPosition = new Vec3(ex, ey, ez);
                TrolldenRespawnState.lastEntityId = existing.getId();
                TrolldenRespawnState.lastLevelKey = level.dimension().location().toString();
                return;
            }

            // ZOMBIE — entity found but corrupt.
            // Save position if still valid (before it becomes NaN).
            System.out.println("[TrolldenRespawn] Zombie detected"
                    + " removed=" + existing.isRemoved()
                    + " alive=" + existing.isAlive()
                    + " NaN=" + coordsNaN
                    + " addedToWorld=" + existing.isAddedToWorld
                    + " invisible=" + existing.isInvisible());

            if (!coordsNaN) {
                TrolldenRespawnState.lastPosition = new Vec3(ex, ey, ez);
                TrolldenRespawnState.lastLevelKey = level.dimension().location().toString();
            }
            TrolldenRespawnState.lastEntityId = existing.getId();
            TrolldenRespawnState.zombieUUID   = existing.getUUID();

            doRespawn(event.getServer(), level);
            return;
        }

        // MISSING — not found in any level.
        // The setRemoved override captured the position before NaN corruption.
        System.out.println("[TrolldenRespawn] Trollden missing from all levels.");
        doRespawn(event.getServer(), null);
    }

    private static void doRespawn(
            net.minecraft.server.MinecraftServer server,
            ServerLevel hintLevel
    ) {
        if (TrolldenRespawnState.lastPosition == null) {
            System.out.println("[TrolldenRespawn] Cannot respawn: no position recorded.");
            return;
        }
        Vec3 pos = TrolldenRespawnState.lastPosition;
        if (Double.isNaN(pos.x) || Double.isNaN(pos.y) || Double.isNaN(pos.z)) {
            System.out.println("[TrolldenRespawn] Cannot respawn: position is NaN.");
            return;
        }

        // Resolve the level — prefer the cached key, fall back to overworld.
        ServerLevel spawnLevel = hintLevel;
        if (spawnLevel == null && TrolldenRespawnState.lastLevelKey != null) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.dimension().location().toString()
                        .equals(TrolldenRespawnState.lastLevelKey)) {
                    spawnLevel = level;
                    break;
                }
            }
        }
        if (spawnLevel == null) {
            spawnLevel = server.overworld();
        }

        // Broadcast a manual remove packet for the last tracked entity ID.
        // If the EntityInLevelCallback was nulled out before addRemoveTask ran
        // (which is what normalattack does), the vanilla remove packet was never
        // sent. Clients are left with a ghost model. This clears it.
        if (TrolldenRespawnState.lastEntityId != -1) {
            ClientboundRemoveEntitiesPacket pkt =
                    new ClientboundRemoveEntitiesPacket(TrolldenRespawnState.lastEntityId);
            for (ServerPlayer player : spawnLevel.players()) {
                player.connection.send(pkt);
            }
        }

        // Ground-snap Y — knockback from the attack may have sent Trollden
        // airborne. The cached Y might be mid-air.
        BlockPos ground = spawnLevel.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos((int) pos.x, 0, (int) pos.z));
        double sx = pos.x, sy = ground.getY(), sz = pos.z;

        // Spawn the replacement.
        TrolldenEntity newTrolden = TrolldenModEntities.TROLLDEN.get().create(spawnLevel);
        if (newTrolden == null) {
            System.out.println("[TrolldenRespawn] Entity creation returned null!");
            return;
        }
        newTrolden.moveTo(sx, sy, sz);
        spawnLevel.addFreshEntity(newTrolden);

        // Point trackedUUID at the new entity BEFORE discarding the zombie.
        // Once trackedUUID no longer matches the zombie's UUID, TrolldenEntity's
        // setRemoved override stops protecting it and discard() goes through.
        UUID zombieUUID = TrolldenRespawnState.zombieUUID;
        TrolldenRespawnState.trackedUUID    = newTrolden.getUUID();
        TrolldenRespawnState.lastEntityId   = newTrolden.getId();
        TrolldenRespawnState.lastPosition   = new Vec3(sx, sy, sz);
        TrolldenRespawnState.zombieUUID     = null;
        TrolldenRespawnState.respawnCooldown = 10; // 0.5s — just enough to let addFreshEntity settle

        if (zombieUUID != null) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity zombie = level.getEntity(zombieUUID);
                if (zombie != null) {
                    zombie.discard();
                    System.out.println("[TrolldenRespawn] Zombie discarded id=" + zombie.getId());
                    break;
                }
            }
        }

        System.out.println("[TrolldenRespawn] Respawned at "
                + sx + "," + sy + "," + sz + " (was Y=" + pos.y + ")"
                + " dim=" + TrolldenRespawnState.lastLevelKey);
    }
}