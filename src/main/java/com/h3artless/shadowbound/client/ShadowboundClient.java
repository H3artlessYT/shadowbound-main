package com.h3artless.shadowbound.client;

import com.h3artless.shadowbound.client.cast.AirCasting;
import com.h3artless.shadowbound.client.cast.ClientCastPoses;
import com.h3artless.shadowbound.client.particle.ShadowParticles;
import com.h3artless.shadowbound.client.cast.PaperScreen;
import com.h3artless.shadowbound.client.cast.CodexScreen;
import com.h3artless.shadowbound.item.ShadowCodexItem;
import com.h3artless.shadowbound.client.render.AirCastFx;
import com.h3artless.shadowbound.client.render.GrimoireRenderer;
import com.h3artless.shadowbound.entity.GrimoireEntity;
import com.h3artless.shadowbound.item.ShadowPaperItem;
import com.h3artless.shadowbound.item.ShadowTridentItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.item.ShadowPouchItem;
import com.h3artless.shadowbound.registry.ModItems;
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
import com.h3artless.shadowbound.client.render.SoulVesselRenderer;
import com.h3artless.shadowbound.client.render.ShadowHookRenderer;
import com.h3artless.shadowbound.client.render.ShadowChestRenderer;
import com.h3artless.shadowbound.client.render.ShadowConsoleRenderer;
import com.h3artless.shadowbound.client.box.BoxScreen;
import com.h3artless.shadowbound.client.box.HoloPanels;
import com.h3artless.shadowbound.client.render.AbyssWatcher;
import com.h3artless.shadowbound.block.ShadowBoxBlockEntity;
import com.h3artless.shadowbound.block.ShadowConsoleBlockEntity;
import com.h3artless.shadowbound.block.ShadowChestBlockEntity;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.util.math.BlockPos;
import com.h3artless.shadowbound.client.render.HookVisuals;
import com.h3artless.shadowbound.client.soul.SoulEmitters;
import com.h3artless.shadowbound.client.soul.SoulTrails;
import com.h3artless.shadowbound.block.SoulVesselBlockEntity;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
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
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import com.h3artless.shadowbound.box.ShadowBoxes;
import com.h3artless.shadowbound.client.render.UmbraSky;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ShadowboundClient implements ClientModInitializer {
    public static KeyBinding spyKey;
    public static KeyBinding diveKey;
    public static KeyBinding snuffKey;
    public static KeyBinding dismissKey;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.SHADOW_CLONE, ShadowCloneRenderer::new);
        EntityRendererRegistry.register(ModEntities.RITUAL, RitualRenderer::new);
        EntityRendererRegistry.register(ModEntities.SOUL_PYLON, SoulPylonRenderer::new);
        EntityRendererRegistry.register(ModEntities.GRIMOIRE, GrimoireRenderer::new);
        EntityRendererRegistry.register(ModEntities.SHADOW_HOOK, ShadowHookRenderer::new);
        EntityRendererRegistry.register(ModEntities.SHADOW_ROBOT, com.h3artless.shadowbound.client.render.ShadowRobotRenderer::new);

        ShadowParticles.init();
        ModelPredicateProviderRegistry.register(ModItems.SHADOW_POUCH, Shadowbound.id("full"),
                (stack, world, entity, seed) -> ShadowPouchItem.isFull(stack) ? 1f : 0f);
        ShadowTridentItem.clientDarkness = ClientShadowState::darkness;
        GrimoireEntity.openCast = AirCasting::start;
        GrimoireEntity.casting = AirCasting::isCasting;
        ShadowCodexItem.openCodex = hand -> MinecraftClient.getInstance().setScreen(new CodexScreen(hand));
        ShadowPaperItem.openPaper = (rite, step) -> MinecraftClient.getInstance().setScreen(new PaperScreen(rite, step));

        FxQuality.load();
        CoreShaderRegistrationCallback.EVENT.register(ShadowShaders::register);
        ModelLoadingPlugin.register(ctx -> {
            ctx.addModels(BladeModels.ALL);
            ctx.modifyModelAfterBake().register((model, mc) -> com.h3artless.shadowbound.client.render.ShadowBoxBody.wrap(mc.id(), model));
        });

        spyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shadowbound.spy", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.shadowbound"));

        diveKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shadowbound.dive", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.shadowbound"));

        snuffKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shadowbound.snuff", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.shadowbound"));
        dismissKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shadowbound.dismiss", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.shadowbound"));

        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof net.minecraft.client.render.entity.PlayerEntityRenderer pr) helper.register(new com.h3artless.shadowbound.client.render.SatchelFeatureRenderer(pr));
        });
        BlockEntityRendererFactories.register(ModBlocks.SNUFFED_LIGHT_ENTITY, SnuffedLightRenderer::new);
        BlockEntityRendererFactories.register(ModBlocks.SOUL_VESSEL_ENTITY, SoulVesselRenderer::new);
        BlockEntityRendererFactories.register(ModBlocks.SHADOW_CHEST_ENTITY, ShadowChestRenderer::new);
        BlockEntityRendererFactories.register(ModBlocks.PEDESTAL_ENTITY, com.h3artless.shadowbound.client.render.PedestalRenderer::new);
        ShadowChestBlockEntity.clientFx = SoulEmitters::chest;
        BlockEntityRendererFactories.register(ModBlocks.SHADOW_CONSOLE_ENTITY, ShadowConsoleRenderer::new);
        BlockEntityRendererFactories.register(ModBlocks.SHADOW_BOX_ENTITY, com.h3artless.shadowbound.client.render.ShadowBoxCreatureRenderer::new);
        ShadowBoxBlockEntity.clientFx = BoxPersonality::tick;
        ShadowConsoleBlockEntity.clientFx = SoulEmitters::console;
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.CONSOLE_OPEN, (client, handler, buf, sender) -> {
            BoxScreen.Data data = BoxScreen.Data.read(buf);
            client.execute(() -> HoloPanels.open(data));
        });
        ShadowChestBlockEntity chestIcon = new ShadowChestBlockEntity(BlockPos.ORIGIN, ModBlocks.SHADOW_CHEST.getDefaultState());
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.SHADOW_ROBOT, new com.h3artless.shadowbound.client.render.ShadowRobotItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(ModBlocks.SHADOW_CHEST, (stack, mode, matrices, buffers, light, overlay) ->
                MinecraftClient.getInstance().getBlockEntityRenderDispatcher().renderEntity(chestIcon, matrices, buffers, light, overlay));
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(),
                ModBlocks.SHADOW_VISAGE, ModBlocks.SOUL_CHOKED_PILLAR, ModBlocks.WAILING_LANTERN, ModBlocks.WEEPING_FACE, ModBlocks.DEMAT_LEVER, ModBlocks.SHADOW_BOX_TOP);
        SoulVesselBlockEntity.clientTicker = SoulEmitters::tick;

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.CONFIG_SYNC, (client, handler, buf, sender) -> {
            PacketByteBuf copy = PacketByteBufs.copy(buf);
            client.execute(() -> com.h3artless.shadowbound.config.ShadowConfig.applyRemote(copy));
        });
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
            SoulTrails.reset();
            DiveVision.reset();
            DiveTrails.reset();
            ClientShadowState.setDarkness(0);
            com.h3artless.shadowbound.config.ShadowConfig.load();
        });

        ClientTickEvents.START_CLIENT_TICK.register(AirCasting::tick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (spyKey.wasPressed()) {
                if (client.player == null) continue;
                if (SpyCamera.locked()) {
                    ClientPlayNetworking.send(ModPackets.SPY_CANCEL, PacketByteBufs.empty());
                    if (!SpyCamera.isActive()) SpyCamera.end();
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
            while (dismissKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null && !SpyCamera.isActive()) {
                    ClientPlayNetworking.send(ModPackets.SHADOW_DISMISS, PacketByteBufs.empty());
                }
            }

            SpyCamera.tick(client);
            DiveVision.tick(client);
            DiveTrails.tick(client);
            DarknessMeterHud.tick(client);
            CooldownHud.tick(client);
            SoulEmitters.refresh(client);
            if (!client.isPaused()) SoulTrails.tick();
            WatchedOverlay.tick(client);
            UmbraSky.tick(client);
        });

        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            WatchedOverlay.render(ctx, tickDelta);
            AirCasting.renderHud(ctx, tickDelta);
            DarknessMeterHud.render(ctx, tickDelta);
            CooldownHud.render(ctx, tickDelta);
            com.h3artless.shadowbound.client.render.PedestalFx.hud(ctx, tickDelta);
        });

        WorldRenderEvents.START.register(ShadowShaders::updateSmoke);
        DimensionRenderingRegistry.registerSkyRenderer(ShadowBoxes.UMBRA, UmbraSky::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(RitualFx::draw);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(SoulPylonFx::draw);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(AirCastFx::draw);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(SoulTrails::draw);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(HookVisuals::drawLasers);
        WorldRenderEvents.AFTER_ENTITIES.register(com.h3artless.shadowbound.client.render.InteriorSpin::draw);
        ClientTickEvents.END_CLIENT_TICK.register(com.h3artless.shadowbound.client.render.InteriorSpin::tick);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(AbyssWatcher::draw);
        ClientTickEvents.END_CLIENT_TICK.register(AbyssWatcher::tick);
        ClientTickEvents.END_CLIENT_TICK.register(com.h3artless.shadowbound.client.render.ShadowBoxBody::tick);
        ClientTickEvents.END_CLIENT_TICK.register(com.h3artless.shadowbound.client.render.RiptideFx::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(com.h3artless.shadowbound.client.render.RiptideFx::draw);
        HoloPanels.init();
        WorldRenderEvents.END.register(ctx -> {
            DiveVision.renderPost(ctx.tickDelta());
            SpyCamera.renderPost(ctx.tickDelta());
        });
    }
}
