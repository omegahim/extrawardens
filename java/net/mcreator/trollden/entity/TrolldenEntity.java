

package net.mcreator.trollden.entity;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

import net.mcreator.trollden.init.TrolldenModEntities;
import net.mcreator.trollden.FixedMeleeAttackGoal;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.Vec3;
import net.mcreator.trollden.procedures.TrolldenOnInitialEntitySpawnProcedure;
import net.mcreator.trollden.TrolldenRespawnState;
import net.minecraft.world.phys.Vec3;
import net.mcreator.trollden.immortal.TrolldenImmuneLevelCallback;
import net.minecraft.world.entity.Pose;


import javax.annotation.Nullable;
import java.util.UUID;

public class TrolldenEntity extends Monster {
	private boolean protectionInitialized = false;
	public TrolldenEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(TrolldenModEntities.TROLLDEN.get(), world);
       	System.out.println("[TrolldenEntity] Constructor called");
        }

   	private boolean deathSwapTriggered = false;

    public UUID   storedUUID;
    public String storedStringUUID;
    public double lastGoodX = 0.0;
    public double lastGoodY = 64.0;
    public double lastGoodZ = 0.0;
    public TrolldenImmuneLevelCallback immuneCallback;

	public void tickDeath() {
	}

	// UUID protection - NEVER let it become null
private UUID protectedUUID = null;

@Override
public UUID getUUID() {
    if (this.uuid == null && protectedUUID != null) {
        this.uuid = protectedUUID;
    } else if (this.uuid != null && protectedUUID == null) {
        protectedUUID = this.uuid;
    }
    return super.getUUID();
}

private int trollPulseTimer = 0;

@Override
public void setUUID(UUID uuid) {
    super.setUUID(uuid);
    if (uuid != null) {
        this.protectedUUID = uuid;
    }
}

@Override
public void onAddedToWorld() {
    super.onAddedToWorld();
    if (!this.level().isClientSide()) {
        TrolldenRespawnState.trackedUUID  = this.getUUID();
        TrolldenRespawnState.lastEntityId = this.getId();
        TrolldenRespawnState.needsRespawn = false;
        double x = this.getX(), y = this.getY(), z = this.getZ();
        if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z)) {
            TrolldenRespawnState.lastPosition = new Vec3(x, y, z);
        }
        if (this.level() instanceof ServerLevel sl) {
            TrolldenRespawnState.lastLevelKey = sl.dimension().location().toString();
        }
    }
}

@Override
public void tick() {

    net.mcreator.trollden.immortal.TrolldenFieldRepair.repairIfNeeded(this);
    net.mcreator.trollden.immortal.TrolldenFieldRepair.recordGoodPosition(this);

   super.tick();
    

        if (!this.level().isClientSide()) {

        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z)) {

            net.mcreator.trollden.immortal.ImmortalData.trollUUID = this.getUUID();

            net.mcreator.trollden.immortal.ImmortalData.lastPosition =
                    new net.minecraft.world.phys.Vec3(x, y, z);
        }
    }

}

    // Store last valid position (your existing logic)

	public double getAttributeValue(Attribute p_21134_) {
		if (p_21134_ == Attributes.MAX_HEALTH) {
			return 500;
		} else {
			return this.getAttributes().getValue(p_21134_);
		}
	}

	public float getHealth() {
		return Mth.clamp(super.getHealth(), 500, getMaxHealth());
	}

	public void setHealth(float health) {
	}

	@Override
	public boolean isDeadOrDying() {
		return false;
	}

// CRITICAL: Override ALL removal methods to block removal
@Override
public void remove(RemovalReason reason) {
    // Block ALL removal - don't call super
    // This is what ChaosWither does
}

@Override
public void setRemoved(RemovalReason reason) {
    // Block ALL removal - don't call super
}

@Override
public RemovalReason getRemovalReason() {
    return null;  // Always return null - never removed
}
  
@Override
public void onRemovedFromWorld() {
    // Block - don't call super
}

@Override
public void die(DamageSource source) {
    // Cancel forced Deathlist kill
    this.setHealth(this.getMaxHealth());
    this.deathTime = 0;
}

@Override
public void kill() {
    // Can't be killed - do nothing
}

    @Override
    public void discard() {
        // Block discard
        System.out.println("[TrolldenEntity] Blocked discard() call");
    }

    

@Override
public boolean canUpdate() {
    return true;  // ALWAYS return true - never stop updating
}

@Override
public void teleportTo(double x, double y, double z) {
    // Block NaN teleportation
    if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
        return; // Do nothing - blocks the NaN teleport
    }
    super.teleportTo(x, y, z);
}

@Override
public void moveTo(double x, double y, double z, float yaw, float pitch) {
    // Block NaN movement
    if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
        return; // Do nothing
    }
    super.moveTo(x, y, z, yaw, pitch);
}

@Override
public void setPos(double x, double y, double z) {
    // Block NaN positioning
    if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
        return; // Do nothing
    }
    super.setPos(x, y, z);
}

	public TrolldenEntity(EntityType<TrolldenEntity> type, Level world) {
		super(type, world);
		setMaxUpStep(0.6f);
		xpReward = 0;
		setNoAi(false);
		setPersistenceRequired();
		this.storedUUID       = this.getUUID();
    	this.storedStringUUID = this.getUUID().toString();
    	TrolldenImmuneLevelCallback.install(this);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new FixedMeleeAttackGoal(this, 1, true, 1, 18));
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
		this.targetSelector.addGoal(6, new NearestAttackableTargetGoal(this, Mob.class, true, true));
		this.targetSelector.addGoal(7, new NearestAttackableTargetGoal(this, AbstractGolem.class, true, true));
		this.targetSelector.addGoal(8, new NearestAttackableTargetGoal(this, Monster.class, true, true));
		this.targetSelector.addGoal(9, new NearestAttackableTargetGoal(this, Animal.class, true, true));
	}

	@Override
	public MobType getMobType() {
		return MobType.UNDEFINED;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public SoundEvent getAmbientSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.warden.agitated"));
	}

	@Override
	public void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.warden.step")), 0.15f, 1);
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.warden.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.warden.death"));
	}

@Override
public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
    return false;
}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata, @Nullable CompoundTag tag) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata, tag);
		TrolldenOnInitialEntitySpawnProcedure.execute(world, this.getX(), this.getY(), this.getZ());
		return retval;
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 500);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 25);
		builder = builder.add(Attributes.FOLLOW_RANGE, 64);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1000);
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 1000);
		return builder;
	}

}
