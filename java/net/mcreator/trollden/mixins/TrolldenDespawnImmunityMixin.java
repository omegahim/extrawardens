package net.mcreator.trollden.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.Entity;

import net.mcreator.trollden.entity.TrolldenEntity;

/**
 * Keeps NULLWardenEntity from being removed on the *server* while still allowing
 * the client to purge and re-sync it (fixes frozen / vibrating warden after respawn).
 */
@Mixin(value = Entity.class, priority = 1000)
public abstract class TrolldenDespawnImmunityMixin {
	/** True only for NULL Warden instances running on the server thread */
	private boolean isServerSideTrollden() {
		Entity self = (Entity) (Object) this;
		return self instanceof TrolldenEntity && !self.level().isClientSide;
	}

	/* ─────────────────────────────  SERVER-SIDE REMOVAL GUARDS  ───────────────────────────── */
	@Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
	private void extrawardens$blockSetRemoved(RemovalReason reason, CallbackInfo ci) {
		if (isServerSideTrollden())
			ci.cancel();
	}

	@Inject(method = "remove", at = @At("HEAD"), cancellable = true)
	private void extrawardens$blockRemove(RemovalReason reason, CallbackInfo ci) {
		if (isServerSideTrollden())
			ci.cancel();
	}

	@Inject(method = "discard", at = @At("HEAD"), cancellable = true)
	private void extrawardens$blockDiscard(CallbackInfo ci) {
		if (isServerSideTrollden())
			ci.cancel();
	}

	@Inject(method = "kill", at = @At("HEAD"), cancellable = true)
	private void extrawardens$blockKill(CallbackInfo ci) {
		if (isServerSideTrollden())
			ci.cancel();
	}

	/* ─────────────────────────────  CLIENT-SIDE VISIBILITY  ───────────────────────────── */
	/** Optional: keep the warden rendered even when far away. Safe to leave enabled. */
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void extrawardens$alwaysRender(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
		Entity self = (Entity) (Object) this;
		if (self instanceof TrolldenEntity) {
			cir.setReturnValue(true);
			cir.cancel();
		}
	}
	
    /**
     * Block onRemovedFromWorld() - Deathlist lines 785, 922
     * This is a Forge method, not vanilla
     */
    @Inject(
        method = "onRemovedFromWorld",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void blockOnRemovedFromWorld(CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;
        if (entity instanceof TrolldenEntity) {
            System.out.println("[AntiDeathlist] Blocked onRemovedFromWorld() call");
            ci.cancel();
        }
    }            
}
