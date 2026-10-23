package com.h3artless.shadowbound.block;

import com.h3artless.shadowbound.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SnuffedLightBlock extends Block implements BlockEntityProvider {
    public SnuffedLightBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    private static BlockState stored(BlockView world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof SnuffedLightBlockEntity be ? be.getStored() : null;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        BlockState s = stored(world, pos);
        return s == null ? VoxelShapes.fullCube() : s.getOutlineShape(world, pos, ctx);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        BlockState s = stored(world, pos);
        return s == null ? VoxelShapes.empty() : s.getCollisionShape(world, pos, ctx);
    }

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        BlockState s = stored(world, pos);
        return s == null ? 1f : s.calcBlockBreakingDelta(player, world, pos);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockState s = stored(world, pos);
        if (!world.isClient && s != null && !player.isCreative()) {
            Block.dropStacks(s, world, pos, null, player, player.getMainHandStack());
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        BlockState s = stored(world, pos);
        return s == null ? ItemStack.EMPTY : new ItemStack(s.getBlock());
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(3) == 0) {
            world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3,
                    pos.getY() + 0.6, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SnuffedLightBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient || type != ModBlocks.SNUFFED_LIGHT_ENTITY) return null;
        return (w, pos, st, be) -> ((SnuffedLightBlockEntity) be).serverTick();
    }
}
