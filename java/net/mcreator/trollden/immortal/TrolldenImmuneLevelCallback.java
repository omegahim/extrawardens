package net.mcreator.trollden.immortal;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;

/**
 * ════════════════════════════════════════════════════════════════
 * TrolldenImmuneLevelCallback
 * ════════════════════════════════════════════════════════════════
 *
 * WHY THIS WORKS — THE CORE INSIGHT:
 *
 *   Every hostile removal system (Deathlist.addRemoveTask, the vanilla
 *   despawn path, chunk unloads, /kill, anything) routes its structural
 *   removal through the entity's f_146801_ field
 *   (Entity.levelCallback / EntityInLevelCallback).
 *
 *   Deathlist.addRemoveTask does:
 *       EntityInLevelCallback var5 = target.f_146801_;
 *       if (var5 instanceof PersistentEntitySectionManager$Callback) {
 *           ... remove from EntityTickList ...
 *           ... remove from EntityLookup ...
 *           ... remove from EntitySectionStorage ...
 *           ... replace f_146801_ with NULL_CALLBACK ...
 *       }
 *
 *   If f_146801_ holds THIS object instead of the real Callback,
 *   that instanceof check is FALSE. The entire removal block is
 *   skipped. Trollden stays in every data structure untouched.
 *
 *   This is not Deathlist-specific. It is how the Minecraft/Forge
 *   entity lifecycle works. Any code that follows the standard
 *   removal contract hits the same instanceof gate. Those that
 *   bypass it (direct field writes) are handled by
 *   TrolldenFieldRepair running every tick.
 *
 * HOW TO INSTALL:
 *   Call TrolldenImmuneLevelCallback.install(this) in TrolldenEntity's
 *   constructor, after super(). The callback holds a reference back to
 *   the entity so it can re-install itself if somehow swapped out.
 *
 * WHAT EACH METHOD DOES:
 *
 *   onTrackingStart()  — called when a callback is first registered.
 *                        We store the position here as our first known-
 *                        good coordinate baseline for field repair.
 *
 *   onTrackingEnd()    — called when the callback is being unregistered.
 *                        We ignore it entirely — Trollden never unregisters.
 *
 *   onRemove(reason)   — the main removal hook. We ignore it entirely.
 *                        Trollden is never removed.
 *
 *   onMove()           — called every time the entity moves to a new
 *                        chunk section. We use this to update our
 *                        stored last-good position.
 */
public class TrolldenImmuneLevelCallback implements EntityInLevelCallback {

    private final TrolldenEntity entity;

    private TrolldenImmuneLevelCallback(TrolldenEntity entity) {
        this.entity = entity;
    }

    /**
     * Install this callback on the entity.
     * Called from TrolldenEntity's constructor.
     */
    public static void install(TrolldenEntity entity) {
        TrolldenImmuneLevelCallback cb = new TrolldenImmuneLevelCallback(entity);
        // m_141960_ = Entity.setLevelCallback(EntityInLevelCallback)
        entity.setLevelCallback(cb);
        // Also stamp the entity's stored instance so EntityCallbackLockMixin can
        // recognise and restore this exact callback if it ever gets swapped out.
        entity.immuneCallback = cb;
    }

    // ── EntityInLevelCallback interface ────────────────────────────────────

    @Override
    public void onMove() {
        // Update last-known-good position whenever Trollden legitimately moves.
        // TrolldenFieldRepair reads this to know what to restore after corruption.
        if (entity != null) {
            TrolldenFieldRepair.recordGoodPosition(entity);
        }
    }

    @Override
    public void onRemove(Entity.RemovalReason reason) {
        // Intentionally empty. Trollden is never removed.
        // Any caller — Deathlist, vanilla despawn, /kill — hits this and exits
        // without touching EntityTickList, EntityLookup, or EntitySectionStorage.
    }
}
