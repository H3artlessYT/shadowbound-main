package com.h3artless.shadowbound.item;

import com.google.common.collect.Multimap;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShadowCleaverItem extends AxeItem implements ShadowBlade {
    public ShadowCleaverItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        TypedActionResult<ItemStack> result = ShadowBlade.release(world, user, hand);
        return result.getResult().isAccepted() ? result : super.use(world, user, hand);
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return BladeReach.withReach(super.getAttributeModifiers(slot), slot);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        ShadowBlade.tooltip(stack, tooltip);
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return ShadowBlade.count(stack) > 0 || super.isItemBarVisible(stack);
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return ShadowBlade.count(stack) > 0 ? ShadowBlade.barStep(stack) : super.getItemBarStep(stack);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return ShadowBlade.count(stack) > 0 ? BAR_COLOR : super.getItemBarColor(stack);
    }
}
