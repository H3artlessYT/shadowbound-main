package com.h3artless.shadowbound.item;

import com.h3artless.shadowbound.registry.ModItems;
import com.h3artless.shadowbound.ritual.ShadowRitual;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

public class ShadowPaperItem extends Item {
    private static final String KEY = "Rite";
    private static final String STEP = "Step";
    private static final String[] NUMERALS = {"I", "II", "III"};

    public static BiConsumer<ShadowRitual, Integer> openPaper = (r, step) -> {};

    public ShadowPaperItem(Settings settings) {
        super(settings);
    }

    @Nullable
    public static ShadowRitual rite(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? null : ShadowRitual.byKey(nbt.getString(KEY));
    }

    public static int step(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : Math.floorMod(nbt.getInt(STEP), ShadowRitual.STEPS);
    }

    public static ItemStack create(ShadowRitual rite, int step) {
        ItemStack stack = new ItemStack(ModItems.SHADOW_PAPER);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(KEY, rite.key());
        nbt.putInt(STEP, Math.floorMod(step, ShadowRitual.STEPS));
        return stack;
    }

    private static void assign(ItemStack stack, World world) {
        if (rite(stack) != null) return;
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(KEY, ShadowRitual.random(world.random).key());
        nbt.putInt(STEP, world.random.nextInt(ShadowRitual.STEPS));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient) assign(stack, world);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            assign(stack, world);
        } else {
            ShadowRitual r = rite(stack);
            if (r != null) {
                user.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.8f, 0.7f);
                openPaper.accept(r, step(stack));
            }
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (rite(stack) != null) {
            tooltip.add(Text.translatable("tooltip.shadowbound.paper.fragment", NUMERALS[step(stack)]).formatted(Formatting.DARK_RED));
        }
        tooltip.add(Text.translatable("tooltip.shadowbound.paper").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }
}
