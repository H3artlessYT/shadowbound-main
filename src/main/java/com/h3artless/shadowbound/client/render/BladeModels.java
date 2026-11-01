package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.registry.ModItems;
import net.fabricmc.fabric.api.client.model.BakedModelManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BladeModels {
    public static final Identifier LONGSWORD_HELD = Shadowbound.id("item/shadow_longsword_handheld");
    public static final Identifier CLEAVER_HELD = Shadowbound.id("item/shadow_cleaver_handheld");
    public static final Identifier TRIDENT_HELD = Shadowbound.id("item/shadow_trident_handheld");
    public static final Identifier TRIDENT_THROWING = Shadowbound.id("item/shadow_trident_throwing");
    public static final Identifier HOOK_BODY = Shadowbound.id("item/shadow_hook_body");
    public static final Identifier HOOK_CLAW = Shadowbound.id("item/shadow_hook_claw");
    public static final List<Identifier> ALL = List.of(LONGSWORD_HELD, CLEAVER_HELD, TRIDENT_HELD, TRIDENT_THROWING, HOOK_BODY, HOOK_CLAW);

    private BladeModels() {}

    public static boolean usesFlatModel(ModelTransformationMode mode) {
        return mode == ModelTransformationMode.GUI || mode == ModelTransformationMode.GROUND || mode == ModelTransformationMode.FIXED;
    }

    private static boolean charging(ItemStack stack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return false;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p.isUsingItem() && p.getActiveItem() == stack) return true;
        }
        return false;
    }

    public static BakedModel get(Identifier id) {
        return BakedModelManagerHelper.getModel(MinecraftClient.getInstance().getBakedModelManager(), id);
    }

    @Nullable
    public static BakedModel heldModelFor(ItemStack stack) {
        Identifier id;
        if (stack.isOf(ModItems.SHADOW_LONGSWORD)) id = LONGSWORD_HELD;
        else if (stack.isOf(ModItems.SHADOW_CLEAVER)) id = CLEAVER_HELD;
        else if (stack.isOf(ModItems.SHADOW_TRIDENT)) id = charging(stack) ? TRIDENT_THROWING : TRIDENT_HELD;
        else return null;
        return BakedModelManagerHelper.getModel(MinecraftClient.getInstance().getBakedModelManager(), id);
    }
}
