package com.h3artless.shadowbound.entity;

import com.h3artless.shadowbound.registry.ModItems;
import com.h3artless.shadowbound.registry.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

public class GrimoireEntity extends Entity {
    public static final float HOVER = 1.15f;
    private static final DustParticleEffect RED = new DustParticleEffect(new Vector3f(0.9f, 0.08f, 0.1f), 0.8f);
    private static final TrackedData<Integer> FIZZLE = DataTracker.registerData(GrimoireEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public static IntConsumer openCast = id -> {};
    public static IntPredicate casting = id -> false;

    public float open, prevOpen, page, prevPage, flipTurn, flipTarget, turn, prevTurn;

    public GrimoireEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(FIZZLE, -100);
    }

    public int getFizzleAge() {
        return this.dataTracker.get(FIZZLE);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) animate();
    }

    private void animate() {
        prevOpen = open;
        prevPage = page;
        prevTurn = turn;
        boolean cast = casting.test(getId());
        PlayerEntity p = getWorld().getClosestPlayer(getX(), getY() + HOVER, getZ(), 4.5, false);
        if (cast || p != null) {
            open = Math.min(1f, open + 0.1f);
            if (open < 0.5f || random.nextInt(cast ? 5 : 40) == 0) {
                float was = flipTarget;
                do {
                    flipTarget += random.nextInt(4) - random.nextInt(4);
                } while (was == flipTarget);
            }
        } else {
            open = Math.max(0f, open - 0.1f);
        }

        float want = turn + 0.02f;
        if (p != null) want = (float) MathHelper.atan2(p.getZ() - getZ(), p.getX() - getX());
        float d = MathHelper.wrapDegrees((want - turn) * MathHelper.DEGREES_PER_RADIAN) * MathHelper.RADIANS_PER_DEGREE;
        turn += d * 0.25f;

        float h = MathHelper.clamp((flipTarget - page) * 0.4f, -0.2f, 0.2f);
        flipTurn += (h - flipTurn) * 0.9f;
        page += flipTurn;

        World w = getWorld();
        double by = getY() + HOVER + (cast ? 0.2 : 0.0);
        if (open > 0.3f && random.nextInt(cast ? 2 : 6) == 0) {
            w.addParticle(ModParticles.RUNE, getX() + (random.nextDouble() - 0.5) * 0.4, by + 0.15, getZ() + (random.nextDouble() - 0.5) * 0.4,
                    0, cast ? 0.02 : 0.0, 0);
        }
        if (random.nextInt(cast ? 3 : 9) == 0) {
            w.addParticle(ModParticles.ASH, getX() + (random.nextDouble() - 0.5) * 0.5, by + 0.1, getZ() + (random.nextDouble() - 0.5) * 0.5,
                    0, cast ? 0.02 : 0.0, 0);
        }
        if (random.nextInt(cast ? 2 : 5) == 0) {
            w.addParticle(ModParticles.MOTE, getX(), by - 0.35 - random.nextDouble() * 0.3, getZ(),
                    cast ? 0.7 + random.nextDouble() * 0.3 : 0.0, cast ? 0.012 : 0.0, 0);
        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (getWorld().isClient && !casting.test(getId())) openCast.accept(getId());
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (getWorld().isClient || isRemoved()) return false;
        if (source.getAttacker() instanceof PlayerEntity player) {
            returnTo(player, false);
            return true;
        }
        return false;
    }

    public void returnTo(PlayerEntity player, boolean cast) {
        if (!(getWorld() instanceof ServerWorld sw) || isRemoved()) return;
        ItemStack book = new ItemStack(ModItems.SHADOW_GRIMOIRE);
        if (!player.getInventory().insertStack(book)) player.dropItem(book, false);
        double y = getY() + HOVER;
        sw.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), y, getZ(), cast ? 18 : 10, 0.2, 0.2, 0.2, 0.02);
        sw.spawnParticles(RED, getX(), y, getZ(), 12, 0.3, 0.3, 0.3, 0.0);
        sw.playSound(null, getX(), y, getZ(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 0.6f);
        if (cast) sw.playSound(null, getX(), y, getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.9f, 0.5f);
        discard();
    }

    public void fizzle() {
        this.dataTracker.set(FIZZLE, age);
        if (getWorld() instanceof ServerWorld sw) {
            double y = getY() + HOVER;
            sw.spawnParticles(ParticleTypes.SMOKE, getX(), y, getZ(), 12, 0.2, 0.15, 0.2, 0.02);
            sw.playSound(null, getX(), y, getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.6f, 0.7f);
        }
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRender(double distance) {
        return distance < 64 * 64;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
