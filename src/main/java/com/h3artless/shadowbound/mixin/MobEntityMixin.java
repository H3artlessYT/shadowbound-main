package com.h3artless.shadowbound.mixin;

import com.h3artless.shadowbound.shadow.ShadowAllies;
import com.h3artless.shadowbound.shadow.ShadowEmbers;
import com.h3artless.shadowbound.shadow.ShadowFollowOwnerGoal;
import com.h3artless.shadowbound.shadow.ShadowMob;
import com.h3artless.shadowbound.shadow.ShadowStrikeGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity implements ShadowMob {
    @Unique
    private static final TrackedData<Boolean> SHADOWBOUND_IS_SHADOW =
            DataTracker.registerData(MobEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Unique @Nullable private UUID shadowbound$owner;
    @Unique private int shadowbound$life;
    @Unique private boolean shadowbound$stale;

    protected MobEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void shadowbound$track(CallbackInfo ci) {
        this.dataTracker.startTracking(SHADOWBOUND_IS_SHADOW, false);
    }

    @Override
    public boolean shadowbound$isShadow() {
        return this.dataTracker.get(SHADOWBOUND_IS_SHADOW);
    }

    @Override
    public @Nullable UUID shadowbound$getOwner() {
        return shadowbound$owner;
    }

    @Override
    public void shadowbound$bind(UUID owner, int lifetime) {
        MobEntity self = (MobEntity) (Object) this;
        this.shadowbound$owner = owner;
        this.shadowbound$life = lifetime;
        this.dataTracker.set(SHADOWBOUND_IS_SHADOW, true);

        MobEntityAccessor acc = (MobEntityAccessor) self;
        wipe(acc.shadowbound$targets());
        self.setTarget(null);

        if (self instanceof PathAwareEntity pathy) {
            if (!self.getAttributes().hasAttribute(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
                acc.shadowbound$goals().add(1, new ShadowStrikeGoal(pathy));
            }
            acc.shadowbound$goals().add(6, new ShadowFollowOwnerGoal(self, () -> shadowbound$owner, 1.2, 8f, 3f));
        }
    }

    @Unique
    private static void wipe(GoalSelector selector) {
        selector.getRunningGoals().forEach(PrioritizedGoal::stop);
        selector.getGoals().clear();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void shadowbound$tick(CallbackInfo ci) {
        if (!shadowbound$isShadow() && !shadowbound$stale) return;
        World world = this.getWorld();

        if (world.isClient) {
            if (this.random.nextInt(6) == 0) {
                world.addParticle(ParticleTypes.SMOKE,
                        this.getParticleX(0.6), this.getRandomBodyY(), this.getParticleZ(0.6), 0, 0.02, 0);
            }
            if (this.random.nextInt(14) == 0) {
                world.addParticle(ShadowEmbers.EMBER,
                        this.getParticleX(0.7), this.getRandomBodyY(), this.getParticleZ(0.7), 0, 0.03, 0);
            }
            return;
        }

        MobEntity self = (MobEntity) (Object) this;
        PlayerEntity owner = shadowbound$owner == null ? null : world.getPlayerByUuid(shadowbound$owner);
        if (shadowbound$stale || owner == null || --shadowbound$life <= 0) {
            fade((ServerWorld) world);
            return;
        }

        if ((this.age + this.getId()) % 5 == 0) {
            ShadowAllies.aim(self, ShadowAllies.pickTarget(self, owner));
        }
    }

    @Unique
    private void fade(ServerWorld world) {
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getBodyY(0.5), getZ(), 20, 0.3, 0.5, 0.3, 0.02);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, getX(), getBodyY(0.5), getZ(), 6, 0.2, 0.3, 0.2, 0.05);
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ALLAY_DEATH, SoundCategory.HOSTILE, 0.5f, 0.4f);
        this.discard();
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void shadowbound$write(NbtCompound nbt, CallbackInfo ci) {
        if (shadowbound$isShadow()) nbt.putBoolean("ShadowboundShadow", true);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void shadowbound$read(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.getBoolean("ShadowboundShadow")) shadowbound$stale = true;
    }
}
