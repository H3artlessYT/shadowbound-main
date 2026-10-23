package com.h3artless.shadowbound.snuff;

import com.h3artless.shadowbound.block.SnuffedLightBlockEntity;
import com.h3artless.shadowbound.dive.ShadowDive;
import com.h3artless.shadowbound.item.TotemOfShadownessItem;
import com.h3artless.shadowbound.meter.DarknessMeter;
import com.h3artless.shadowbound.registry.ModBlocks;
import com.h3artless.shadowbound.spy.SpyManager;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.event.GameEvent;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SnuffOut {
    public static final int COST = 400;
    public static final int RADIUS = 16;
    public static final int DARK_TICKS = 20 * 60;
    private static final int COOLDOWN = 20 * 20;
    private static final int MAX_BLOCKS = 400;
    private static final float WAVE_SPEED = 0.9f;

    private static final DustParticleEffect RED = new DustParticleEffect(new Vector3f(0.85f, 0.06f, 0.1f), 1.2f);

    private static final List<Wave> WAVES = new ArrayList<>();
    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    private record Target(BlockPos pos, double dist) {}

    private static final class Wave {
        final ServerWorld world;
        final Vec3d center;
        final Deque<Target> todo;
        float radius;
        int sounds;
        int ticks;

        Wave(ServerWorld world, Vec3d center, Deque<Target> todo) {
            this.world = world;
            this.center = center;
            this.todo = todo;
        }
    }

    private SnuffOut() {}

    public static void use(ServerPlayerEntity p) {
        if (!p.isAlive() || p.isSpectator() || SpyManager.isSpying(p) || ShadowDive.isDiving(p)) return;
        if (!TotemOfShadownessItem.isHolding(p)) return;
        long now = p.getServerWorld().getTime();
        Long readyAt = READY_AT.get(p.getUuid());
        if (readyAt != null && now < readyAt) return;
        if (DarknessMeter.get(p) < COST) {
            p.sendMessage(Text.translatable("message.shadowbound.snuff.no_darkness").formatted(Formatting.GRAY), true);
            return;
        }

        ServerWorld world = p.getServerWorld();
        Deque<Target> todo = scan(world, p.getBlockPos());
        DarknessMeter.set(p, DarknessMeter.get(p) - COST);
        READY_AT.put(p.getUuid(), now + COOLDOWN);
        WAVES.add(new Wave(world, p.getPos(), todo));

        world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 0.6f, 1.6f);
        world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.PLAYERS, 0.5f, 1.8f);
        world.spawnParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.5, 0.4, 0.15);
        if (todo.isEmpty()) {
            p.sendMessage(Text.translatable("message.shadowbound.snuff.nothing").formatted(Formatting.GRAY), true);
        }
    }

    private static Deque<Target> scan(ServerWorld world, BlockPos center) {
        List<Target> found = new ArrayList<>();
        int r = RADIUS, r2 = r * r;
        int ox = center.getX(), oy = center.getY(), oz = center.getZ();
        int minY = Math.max(world.getBottomY(), oy - r), maxY = Math.min(world.getTopY() - 1, oy + r);
        for (int cx = (ox - r) >> 4; cx <= (ox + r) >> 4; cx++) {
            for (int cz = (oz - r) >> 4; cz <= (oz + r) >> 4; cz++) {
                if (!world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                WorldChunk chunk = world.getChunk(cx, cz);
                ChunkSection[] sections = chunk.getSectionArray();
                for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
                    int idx = chunk.sectionCoordToIndex(sy);
                    if (idx < 0 || idx >= sections.length) continue;
                    ChunkSection sec = sections[idx];
                    if (sec.isEmpty() || !sec.hasAny(GLOWS)) continue;
                    int x0 = Math.max(cx << 4, ox - r), x1 = Math.min((cx << 4) + 15, ox + r);
                    int z0 = Math.max(cz << 4, oz - r), z1 = Math.min((cz << 4) + 15, oz + r);
                    int y0 = Math.max(sy << 4, minY), y1 = Math.min((sy << 4) + 15, maxY);
                    for (int y = y0; y <= y1; y++) {
                        int dy = y - oy;
                        for (int z = z0; z <= z1; z++) {
                            int dz = z - oz;
                            for (int x = x0; x <= x1; x++) {
                                int dx = x - ox;
                                int d2 = dx * dx + dy * dy + dz * dz;
                                if (d2 > r2) continue;
                                BlockState st = sec.getBlockState(x & 15, y & 15, z & 15);
                                if (st.getLuminance() <= 0 || !canSnuff(st)) continue;
                                found.add(new Target(new BlockPos(x, y, z), Math.sqrt(d2)));
                            }
                        }
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(Target::dist));
        if (found.size() > MAX_BLOCKS) found = found.subList(0, MAX_BLOCKS);
        return new ArrayDeque<>(found);
    }

    private static final java.util.function.Predicate<BlockState> GLOWS = st -> st.getLuminance() > 0;

    private static boolean litCandleOrCampfire(BlockState s) {
        return (s.getBlock() instanceof AbstractCandleBlock || s.getBlock() instanceof CampfireBlock || s.isOf(Blocks.CANDLE_CAKE))
                && s.contains(Properties.LIT) && s.get(Properties.LIT);
    }

    private static boolean canSnuff(BlockState s) {
        if (litCandleOrCampfire(s)) return true;
        if (s.hasBlockEntity() || s.emitsRedstonePower()) return false;
        if (s.contains(Properties.LIT) || s.contains(Properties.POWERED)) return false;
        if (!s.getFluidState().isEmpty()) return false;
        if (s.getBlock() instanceof AbstractFireBlock || s.getBlock() instanceof NetherPortalBlock
                || s.getBlock() instanceof RespawnAnchorBlock) return false;
        if (s.isOf(Blocks.LIGHT) || s.isOf(Blocks.END_PORTAL) || s.isOf(Blocks.END_GATEWAY)) return false;
        return !s.isOf(ModBlocks.SNUFFED_LIGHT);
    }

    public static void tick(MinecraftServer server) {
        for (Iterator<Wave> it = WAVES.iterator(); it.hasNext(); ) {
            Wave w = it.next();
            w.radius += WAVE_SPEED;
            if ((w.ticks++ & 1) == 0) ring(w);
            while (!w.todo.isEmpty() && w.todo.peekFirst().dist() <= w.radius) {
                snuff(w, w.todo.pollFirst().pos());
            }
            if (w.radius > RADIUS + 1) it.remove();
        }
    }

    private static void ring(Wave w) {
        int n = MathHelper.clamp((int) (w.radius * 2.2f), 6, 32);
        for (int i = 0; i < n; i++) {
            double a = i / (double) n * Math.PI * 2 + w.radius * 0.1;
            double x = w.center.x + Math.cos(a) * w.radius, z = w.center.z + Math.sin(a) * w.radius;
            w.world.spawnParticles(i % 3 == 0 ? RED : ParticleTypes.LARGE_SMOKE, x, w.center.y + 0.2, z, 1, 0.05, 0.1, 0.05, 0.0);
        }
    }

    private static void snuff(Wave w, BlockPos pos) {
        ServerWorld world = w.world;
        BlockState s = world.getBlockState(pos);
        if (s.getLuminance() <= 0 || !canSnuff(s)) return;

        if (litCandleOrCampfire(s)) {
            world.setBlockState(pos, s.with(Properties.LIT, false), Block.NOTIFY_ALL);
            world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
        } else {
            world.setBlockState(pos, ModBlocks.SNUFFED_LIGHT.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            if (world.getBlockEntity(pos) instanceof SnuffedLightBlockEntity be) {
                be.setup(s, DARK_TICKS + world.random.nextInt(60));
                world.updateListeners(pos, ModBlocks.SNUFFED_LIGHT.getDefaultState(), ModBlocks.SNUFFED_LIGHT.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }

        world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 4, 0.15, 0.1, 0.15, 0.02);
        if (w.sounds < 24) {
            w.sounds++;
            world.playSound(null, pos, w.sounds % 2 == 0 ? SoundEvents.BLOCK_CANDLE_EXTINGUISH : SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.BLOCKS, 0.5f, 0.8f + world.random.nextFloat() * 0.4f);
        }
    }
}
