package com.h3artless.shadowbound.item;

import com.h3artless.shadowbound.entity.GrimoireEntity;
import com.h3artless.shadowbound.registry.ModEntities;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShadowGrimoireItem extends Item {
    public ShadowGrimoireItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world instanceof ServerWorld server) {
            HitResult hit = user.raycast(5.0, 0f, false);
            Vec3d at;
            if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
                at = Vec3d.ofBottomCenter(bh.getBlockPos().offset(bh.getSide()));
            } else {
                Vec3d look = user.getRotationVector().multiply(1, 0, 1).normalize().multiply(1.6);
                at = user.getPos().add(look);
            }
            GrimoireEntity book = ModEntities.GRIMOIRE.create(server);
            if (book == null) return TypedActionResult.pass(stack);
            book.refreshPositionAndAngles(at.x, at.y, at.z, user.getYaw() + 180f, 0f);
            server.spawnEntity(book);
            server.playSound(null, at.x, at.y + GrimoireEntity.HOVER, at.z, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 0.5f);
            server.playSound(null, at.x, at.y + GrimoireEntity.HOVER, at.z, SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, SoundCategory.PLAYERS, 0.6f, 0.7f);
            if (!user.getAbilities().creativeMode) stack.decrement(1);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.shadowbound.grimoire").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }
}
