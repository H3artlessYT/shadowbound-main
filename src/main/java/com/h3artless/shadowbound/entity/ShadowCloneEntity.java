package com.h3artless.shadowbound.entity;

import com.h3artless.shadowbound.clone.CloneManager;
import com.h3artless.shadowbound.clone.Taunts;
import com.h3artless.shadowbound.shadow.ShadowAllies;
import com.h3artless.shadowbound.shadow.ShadowEmbers;
import com.h3artless.shadowbound.shadow.ShadowFollowOwnerGoal;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class ShadowCloneEntity extends PathAwareEntity {
    private static final TrackedData<Optional<UUID>> OWNER =
            DataTracker.registerData(ShadowCloneEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    private int tauntTimer = 3600;
    private int stepSound;
    private boolean ownerWasSwinging;

    public ShadowCloneEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setPersistent();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setEquipmentDropChance(slot, 0f);
        }
    }

    public static DefaultAttributeContainer.Builder createCloneAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.33)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 4.0);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(OWNER, Optional.empty());
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.3, true) {
            private int swing;

            @Override
            public void tick() {
                super.tick();
                if (swing > 0) swing--;
            }

            @Override
            protected void attack(LivingEntity target, double squaredDistance) {
                if (squaredDistance <= getSquaredMaxAttackDistance(target) && swing <= 0) {
                    swing = swingTicks();
                    tryAttack(target);
                }
            }
        });
        this.goalSelector.add(5, new ShadowFollowOwnerGoal(this, this::getOwnerUuid, 1.15, 5f, 2.5f));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
        this.goalSelector.add(9, new LookAroundGoal(this));
    }

    public void setup(PlayerEntity owner) {
        this.dataTracker.set(OWNER, Optional.of(owner.getUuid()));

        copyGear(owner);
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.dataTracker.get(OWNER).orElse(null);
    }

    @Nullable
    public PlayerEntity getOwner() {
        UUID id = getOwnerUuid();
        return id == null ? null : getWorld().getPlayerByUuid(id);
    }

    @Override
    public void tick() {
        super.tick();

        if (getWorld().isClient) {
            clientTick();
            return;
        }

        PlayerEntity owner = getOwner();
        if (owner == null || !CloneManager.isCurrent(owner, this)) {
            poof();
            return;
        }

        if (this.age % 5 == 0) {
            ShadowAllies.aim(this, ShadowAllies.pickTarget(this, owner));
        }
        mirror(owner);
        if (this.age % 10 == 0) copyGear(owner);

        double moved = getVelocity().horizontalLengthSquared();
        if (moved > 0.002 && ++stepSound >= 9) {
            stepSound = 0;
            playSound(SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, 0.25f, 0.55f + random.nextFloat() * 0.1f);
        }

        if (--tauntTimer <= 0) {
            tauntTimer = 6000 + random.nextInt(6000);
            Taunts.mouthOff(this, owner);
        }
    }

    private void clientTick() {
        if (random.nextInt(6) == 0) {
            getWorld().addParticle(ParticleTypes.SMOKE, getParticleX(0.5), getRandomBodyY(), getParticleZ(0.5), 0, 0.02, 0);
        }
        if (random.nextInt(12) == 0) {
            getWorld().addParticle(ShadowEmbers.EMBER, getParticleX(0.6), getRandomBodyY(), getParticleZ(0.6), 0, 0.03, 0);
        }
    }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public int swingTicks() {
        double speed = Math.max(0.25, getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED));
        return MathHelper.clamp((int) Math.round(20.0 / speed), 2, 100);
    }

    private void copyGear(PlayerEntity owner) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack theirs = owner.getEquippedStack(slot);
            if (!com.h3artless.shadowbound.config.ShadowConfig.shadowMayHold(theirs)) theirs = ItemStack.EMPTY;
            ItemStack mine = getEquippedStack(slot);
            if (ItemStack.areEqual(theirs, mine)) continue;
            equipStack(slot, theirs.copy());
        }
    }

    @Override
    public void tickMovement() {
        this.tickHandSwing();
        super.tickMovement();
    }

    private void mirror(PlayerEntity owner) {
        boolean fighting = getTarget() != null;
        boolean crouch = !fighting && owner.isInSneakingPose();
        EntityPose want = crouch ? EntityPose.CROUCHING : EntityPose.STANDING;
        if (getPose() != want) setPose(want);
        setSneaking(crouch);
        setSprinting(fighting || owner.isSprinting());

        boolean ownerSwinging = owner.handSwinging;
        if (!fighting && ownerSwinging && !ownerWasSwinging && squaredDistanceTo(owner) < 16 * 16) {
            swingHand(Hand.MAIN_HAND);
        }
        ownerWasSwinging = ownerSwinging;
    }

    public void poof() {
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getBodyY(0.5), getZ(), 25, 0.3, 0.6, 0.3, 0.02);
            sw.spawnParticles(ParticleTypes.REVERSE_PORTAL, getX(), getBodyY(0.5), getZ(), 30, 0.3, 0.6, 0.3, 0.1);
            sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 0.7f, 0.6f);
        }
        discard();
    }

    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        swingHand(Hand.MAIN_HAND);
        boolean hit = super.tryAttack(target);
        boolean fist = getMainHandStack().isEmpty();
        boolean sweeps = getMainHandStack().getItem() instanceof net.minecraft.item.SwordItem;
        if (hit && fist) {
            playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 0.8f, 0.9f + random.nextFloat() * 0.2f);
        } else if (hit && !sweeps) {
            playSound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 0.9f + random.nextFloat() * 0.2f);
        } else if (hit && getWorld() instanceof ServerWorld sw) {
            float yaw = getYaw() * MathHelper.RADIANS_PER_DEGREE;
            double dx = -MathHelper.sin(yaw), dz = MathHelper.cos(yaw);
            sw.spawnParticles(ParticleTypes.SWEEP_ATTACK, getX() + dx, getBodyY(0.5), getZ() + dz, 0, dx, 0, dz, 0);
            playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.8f + random.nextFloat() * 0.2f);
        } else if (!hit) {
            playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.6f, 0.9f);
        }
        if (hit && !getWorld().isClient && target instanceof LivingEntity living && !living.isAlive()) {
            PlayerEntity owner = getOwner();
            if (owner != null) Taunts.onKill(this, owner, living);
        }
        return hit;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt && !getWorld().isClient && isAlive() && random.nextInt(20) == 0) {
            PlayerEntity owner = getOwner();
            if (owner != null) Taunts.onHurt(this, owner);
        }
        return hurt;
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!getWorld().isClient) {
            PlayerEntity owner = getOwner();
            if (owner != null) {
                Taunts.onDeath(this, owner);
                CloneManager.onCloneDied(owner);
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PHANTOM_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropXp() {
        return false;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("Owner")) {
            this.dataTracker.set(OWNER, Optional.of(nbt.getUuid("Owner")));
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        UUID id = getOwnerUuid();
        if (id != null) nbt.putUuid("Owner", id);
    }
}