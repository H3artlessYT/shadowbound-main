package com.h3artless.shadowbound.block;

import com.h3artless.shadowbound.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class SnuffedLightBlockEntity extends BlockEntity {
    private BlockState stored = Blocks.AIR.getDefaultState();
    private long relightAt = -1;
    private int pending;

    public SnuffedLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SNUFFED_LIGHT_ENTITY, pos, state);
    }

    public void setup(BlockState original, int duration) {
        this.stored = original;
        this.pending = duration;
        this.relightAt = world == null ? -1 : world.getTime() + duration;
        markDirty();
    }

    public BlockState getStored() {
        return stored;
    }

    void serverTick() {
        if (!(world instanceof ServerWorld sw)) return;
        if (relightAt < 0) {
            relightAt = sw.getTime() + pending;
            markDirty();
        }
        if (sw.getTime() < relightAt) return;
        BlockState back = stored.isAir() ? Blocks.AIR.getDefaultState() : stored;
        if (!back.canPlaceAt(sw, pos)) {
            Block.dropStacks(back, sw, pos);
            back = Blocks.AIR.getDefaultState();
        }
        sw.setBlockState(pos, back, Block.NOTIFY_ALL);
        if (!back.isAir()) {
            sw.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.01);
            sw.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 0.3f, 0.6f + sw.random.nextFloat() * 0.3f);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put("Stored", NbtHelper.fromBlockState(stored));
        nbt.putLong("RelightAt", relightAt);
        nbt.putInt("TicksLeft", pending);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        stored = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("Stored"));
        relightAt = nbt.contains("RelightAt") ? nbt.getLong("RelightAt") : -1;
        pending = nbt.getInt("TicksLeft");
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
