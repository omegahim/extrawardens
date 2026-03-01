package net.mcreator.trollden;

import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public class TrolldenRespawnState {

    /** UUID of the currently protected Trollden. All overrides check this. */
    public static UUID trackedUUID = null;

    /**
     * UUID of an entity that was blocked from being removed and needs to be
     * cleanly discarded once a replacement has been spawned. Set by the
     * setRemoved override, cleared by the respawn handler after discard().
     */
    public static UUID zombieUUID = null;

    /**
     * Numeric entity ID of the last known live Trollden.
     * Used to broadcast a manual ClientboundRemoveEntitiesPacket, because
     * normalattack() nulls the EntityInLevelCallback before addRemoveTask(),
     * causing the vanilla remove packet to never reach clients.
     * Even though the Deathlist mixin now blocks normalattack, this is kept
     * as a safety net for any ghost that slips through.
     */
    public static int lastEntityId = -1;

    /**
     * Last known valid (non-NaN) position.
     * Updated every tick while alive by the handler, and captured by the
     * setRemoved override as a final safety snapshot.
     */
    public static Vec3 lastPosition = null;

    /** Dimension resource key where Trollden was last seen alive. */
    public static String lastLevelKey = null;

    /** Signals the respawn handler that a replacement needs to be spawned. */
    public static boolean needsRespawn = false;

    /** Ticks to wait after a respawn before checking again. */
    public static int respawnCooldown = 0;
}