/**
 * The code of this mod element is always locked.
 *
 * You can register new events in this class too.
 *
 * If you want to make a plain independent class, create it using
 * Project Browser -> New... and make sure to make the class
 * outside net.mcreator.extrawardens as this package is managed by MCreator.
 *
 * If you change workspace package, modid or prefix, you will need
 * to manually adapt this file to these changes or remake it.
 *
 * This class will be added in the mod root package.
*/
package net.mcreator.trollden;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import net.minecraft.world.entity.monster.warden.Warden;

public class FixedMeleeAttackGoal extends Goal {
    protected final PathfinderMob mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private int ticksUntilNextAttack;
    private final double attackReach;
    private final int attackInterval;

    public FixedMeleeAttackGoal(PathfinderMob mob, double speed, boolean mustSee, double reach, int attackInterval) {
        this.mob = mob;
        this.speedModifier = speed;
        this.followingTargetEvenIfNotSeen = mustSee;
        this.attackReach = reach;
        this.attackInterval = attackInterval;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
        return this.mob.isAlive() && this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public void start() {
        this.mob.getNavigation().moveTo(this.mob.getTarget(), this.speedModifier);
        this.mob.setAggressive(true);
        this.ticksUntilNextAttack = 0;
    }

    public void stop() {
        LivingEntity livingentity = this.mob.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
            this.mob.setTarget(null);
        }
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity != null) {
            this.mob.getLookControl().setLookAt(livingentity, 360, 360);
            if (this.mob.tickCount % 10 == 0) {
                if (this.followingTargetEvenIfNotSeen) {
                    this.mob.getNavigation().moveTo(livingentity, this.speedModifier);
                } else if (this.mob.getSensing().hasLineOfSight(livingentity)) {
                    this.mob.getNavigation().moveTo(livingentity, this.speedModifier);
                } else {
                    this.mob.getNavigation().stop();
                }
            }
            if (this.mob.getBoundingBox().inflate(this.getMeleeRange() + 0.6).intersects(livingentity.getBoundingBox()) && this.ticksUntilNextAttack <= 0) {
                this.mob.setDeltaMovement(new Vec3(livingentity.getX(), livingentity.getY(), livingentity.getZ()).subtract(this.mob.position()).normalize().multiply(this.speedModifier * 0.3, 0, this.speedModifier * 0.3).add(0, 0.03, 0));
            }
            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
            this.checkAndPerformAttack(livingentity);
        }
    }

    protected void checkAndPerformAttack(LivingEntity entity) {
        if (this.inMeleeRange(entity) && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(entity);
        }

    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.attackInterval;
    }

    protected boolean inMeleeRange(LivingEntity entity) {
        return this.mob.getBoundingBox().inflate(this.getMeleeRange()).intersects(entity.getBoundingBox());
    }

    public double getMeleeRange() {
        return this.attackReach;
    }
}

