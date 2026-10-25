package com.h3artless.shadowbound.client;

import com.h3artless.shadowbound.client.cast.AirCasting;
import com.h3artless.shadowbound.client.cast.ClientCastPoses;
import com.h3artless.shadowbound.client.particle.ShadowParticles;
import com.h3artless.shadowbound.client.cast.PaperScreen;
import com.h3artless.shadowbound.client.render.AirCastFx;
import com.h3artless.shadowbound.client.render.GrimoireRenderer;
import com.h3artless.shadowbound.entity.GrimoireEntity;
import com.h3artless.shadowbound.item.ShadowPaperItem;
import net.minecraft.client.MinecraftClient;
import com.h3artless.shadowbound.client.hud.CooldownHud;
import com.h3artless.shadowbound.client.hud.DarknessMeterHud;
import com.h3artless.shadowbound.cooldown.Cooldowns;
import com.h3artless.shadowbound.client.hud.WatchedOverlay;
import com.h3artless.shadowbound.client.render.BladeModels;
import com.h3artless.shadowbound.client.render.FxQuality;
import com.h3artless.shadowbound.client.render.RitualFx;
import com.h3artless.shadowbound.client.render.RitualRenderer;
import com.h3artless.shadowbound.client.render.ShadowCloneRenderer;
import com.h3artless.shadowbound.client.render.ShadowShaders;
import com.h3artless.shadowbound.client.dive.DiveTrails;
import com.h3artless.shadowbound.client.render.SnuffedLightRenderer;
import com.h3artless.shadowbound.client.render.SoulPylonFx;
import com.h3artless.shadowbound.client.render.SoulPylonRenderer;
import com.h3artless.shadowbound.registry.ModBlocks;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import com.h3artless.shadowbound.client.dive.DiveVision;
import com.h3artless.shadowbound.client.spy.SpyCamera;
import com.h3artless.shadowbound.client.spy.SpyScreen;
import com.h3artless.shadowbound.item.TotemOfShadownessItem;
import net.minecraft.network.PacketByteBuf;
import com.h3artless.shadowbound.net.ModPackets;
import com.h3artless.shadowbound.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ShadowboundClient implements ClientModInitializer {
    public static KeyBinding spyKey;
    public static KeyBinding diveKey;
    public static KeyBinding snuffKey;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.SHADOW_CLONE, ShadowCloneRenderer::new);
        EntityRendererRegistry.register(ModEntities.RITUAL, RitualRenderer::new);
        EntityRendererRegistry.register(ModEntities.SOUL_PYLON, SoulPylonRenderer::new);
        EntityRendererRegistry.register(ModEntities.GRIMOIRE, GrimoireRenderer::new);

        ShadowParticles.init();
        GrimoireEntity.openCast = AirCasting::start;
        GrimoireEntity.casting = AirCasting::isCasting;
        ShadowPaperItem.openPaper = (rite, step) -> MinecraftClient.getInstance().setScreen(new PaperScreen(rite, step));

        FxQuality.load();
        CoreShaderRegistrationCallback.EVENT.register(ShadowShaders::register);
        ModelLoadingPlugin.register(ctx -> ctx.addModels(BladeModels.ALL));

        spyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shadowbound.spy", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.shadowbound"));

        diveKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shadowbound.dive", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.shadowbound"));

        snuffKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shadowbound.snuff", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.shadowbound"));
        BlockEntityRendererFactories.register(ModBlocks.SNUFFED_LIGHT_ENTITY, SnuffedLightRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.METER_SYNC, (client, handler, buf, sender) -> {
            int value = buf.readVarInt();
            client.execute(() -> ClientShadowState.setDarkness(value));
        });
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.SPY_LIST, (client, handler, buf, sender) -> {
            PacketByteBuf copy = PacketByteBufs.copy(buf);
            client.execute(() -> SpyScreen.receiveList(copy));
        });
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.SPY_START, (client, handler, buf, sender) -> {
            int id = buf.readVarInt();
            client.execute(() -> SpyCamera.begin(id));
        });
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.COOLDOWN_SYNC, (client, handler, buf, sender) -> {
            int n = buf.readVarInt();
            int[] values = new int[n];
            for (int i = 0; i < n; i++) values[i] = buf.readVarInt();
            client.execute(() -> {
                if (client.player != null) Cooldowns.apply(client.player, values);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.CAST_POSE_SYNC, (client, handler, buf, sender) -> {
            int id = buf.readVarInt();
            boolean on = buf.readBoolean();
            client.execute(() -> ClientCastPoses.set(id, on));
        });
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.SPY_STOP, (client, handler, buf, sender) ->
                client.execute(SpyCamera::end));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SpyCamera.reset();
            AirCasting.reset();
            ClientCastPoses.clear();
            DiveVision.reset();
            DiveTrails.reset();
            ClientShadowState.setDarkness(0);
        });

        ClientTickEvents.START_CLIENT_TICK.register(AirCasting::tick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (spyKey.wasPressed()) {
                if (client.player == null) continue;
                if (SpyCamera.isActive()) {
                    ClientPlayNetworking.send(ModPackets.SPY_CANCEL, PacketByteBufs.empty());
                } else if (client.currentScreen == null && TotemOfShadownessItem.isHolding(client.player)) {
                    client.setScreen(new SpyScreen());
                }
            }
            while (diveKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null && !SpyCamera.isActive()) {
                    ClientPlayNetworking.send(ModPackets.DIVE_TOGGLE, PacketByteBufs.empty());
                }
            }
            while (snuffKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null && !SpyCamera.isActive()) {
                    ClientPlayNetworking.send(ModPackets.SNUFF, PacketByteBufs.empty());
                }
            }
            SpyCamera.tick(client);
            DiveVision.tick(client);
            DiveTrails.tick(client);
            DarknessMeterHud.tick(client);
            CooldownHud.tick(client);
            WatchedOverlay.tick(client);
        });

        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            WatchedOverlay.render(ctx, tickDelta);
            AirCasting.renderHud(ctx, tickDelta);
            DarknessMeterHud.render(ctx, tickDelta);
            CooldownHud.render(ctx, tickDelta);
        });

        WorldRenderEvents.START.register(ShadowShaders::updateSmoke);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(RitualFx::draw);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(SoulPylonFx::draw);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(AirCastFx::draw);
        WorldRenderEvents.END.register(ctx -> {
            DiveVision.renderPost(ctx.tickDelta());
            SpyCamera.renderPost(ctx.tickDelta());
        });
    }
}
