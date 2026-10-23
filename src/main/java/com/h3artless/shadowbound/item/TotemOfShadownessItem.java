package com.h3artless.shadowbound.item;

import com.h3artless.shadowbound.registry.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TotemOfShadownessItem extends Item {
    public TotemOfShadownessItem(Settings settings) {
        super(settings);
    }

    public static boolean isHolding(PlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.TOTEM_OF_SHADOWNESS)
                || player.getOffHandStack().isOf(ModItems.TOTEM_OF_SHADOWNESS);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.shadowbound.totem.0").formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.translatable("tooltip.shadowbound.totem.2").formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.translatable("tooltip.shadowbound.totem.3").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.shadowbound.totem.1").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }
}
