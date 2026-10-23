package com.h3artless.shadowbound;

import com.h3artless.shadowbound.clone.CloneManager;
import com.h3artless.shadowbound.item.ShadowBlade;
import com.h3artless.shadowbound.meter.DarknessMeter;
import com.h3artless.shadowbound.net.ModPackets;
import com.h3artless.shadowbound.registry.ModEffects;
import com.h3artless.shadowbound.registry.ModEntities;
import com.h3artless.shadowbound.registry.ModItems;
import com.h3artless.shadowbound.shadow.ShadowAllies;
import com.h3artless.shadowbound.spy.SpyManager;
import com.h3artless.shadowbound.dive.ShadowDive;
import com.h3artless.shadowbound.registry.ModBlocks;
import com.h3artless.shadowbound.registry.ModEnchantments;
import com.h3artless.shadowbound.snuff.SnuffOut;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Shadowbound implements ModInitializer {
    public static final String MOD_ID = "shadowbound";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModItems.init();
        ModBlocks.init();
        ModEntities.init();
        ModEffects.init();
        ModEnchantments.init();
        ModPackets.initServer();

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed) -> {
            if (killer instanceof ServerPlayerEntity player) {
                ShadowBlade.onKill(world, player, killed);
            }
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof PlayerEntity p && ShadowDive.isDiving(p) && source.isIn(DamageTypeTags.IS_FALL)) {
                return false;
            }
            if (source.getAttacker() instanceof LivingEntity attacker) {
                return !ShadowAllies.sameSide(entity, attacker);
            }
            return true;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                ShadowDive.isDiving(player) ? ActionResult.FAIL : ActionResult.PASS);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) ->
                ShadowDive.isDiving(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseBlockCallback.EVENT.register((player, world, hand, hit) ->
                ShadowDive.isDiving(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                ShadowDive.isDiving(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseItemCallback.EVENT.register((player, world, hand) ->
                ShadowDive.isDiving(player) ? TypedActionResult.fail(player.getStackInHand(hand)) : TypedActionResult.pass(player.getStackInHand(hand)));

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                DarknessMeter.copy(oldPlayer, newPlayer));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SpyManager.onDisconnect(handler.player);
            CloneManager.forget(handler.player);
            DarknessMeter.forget(handler.player);
            ShadowDive.forget(handler.player);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SpyManager.onJoin(handler.player));

        ServerTickEvents.END_SERVER_TICK.register(SnuffOut::tick);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                DarknessMeter.tick(player);
                CloneManager.tick(player);
                SpyManager.tick(player);
                ShadowDive.tick(player);
            }
        });

        LOGGER.info("the lights are off");
    }
}
