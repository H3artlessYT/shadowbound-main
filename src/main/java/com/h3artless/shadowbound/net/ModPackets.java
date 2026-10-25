package com.h3artless.shadowbound.net;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.dive.ShadowDive;
import com.h3artless.shadowbound.ritual.RitualCasting;
import com.h3artless.shadowbound.ritual.CastPoses;
import com.h3artless.shadowbound.ritual.RitualPatterns;
import com.h3artless.shadowbound.ritual.ShadowRitual;
import com.h3artless.shadowbound.snuff.SnuffOut;
import com.h3artless.shadowbound.spy.SpyManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ModPackets {
    public static final Identifier METER_SYNC = Shadowbound.id("meter_sync");
    public static final Identifier SPY_LIST = Shadowbound.id("spy_list");
    public static final Identifier SPY_START = Shadowbound.id("spy_start");
    public static final Identifier SPY_STOP = Shadowbound.id("spy_stop");

    public static final Identifier SPY_LIST_REQUEST = Shadowbound.id("spy_list_request");
    public static final Identifier SPY_REQUEST = Shadowbound.id("spy_request");
    public static final Identifier SPY_CANCEL = Shadowbound.id("spy_cancel");
    public static final Identifier DIVE_TOGGLE = Shadowbound.id("dive_toggle");
    public static final Identifier SNUFF = Shadowbound.id("snuff");
    public static final Identifier CAST = Shadowbound.id("cast");
    public static final Identifier COOLDOWN_SYNC = Shadowbound.id("cooldown_sync");
    public static final Identifier CAST_POSE = Shadowbound.id("cast_pose");
    public static final Identifier CAST_POSE_SYNC = Shadowbound.id("cast_pose_sync");

    private ModPackets() {}

    public static void initServer() {
        ServerPlayNetworking.registerGlobalReceiver(SPY_LIST_REQUEST, (server, player, handler, buf, sender) ->
                server.execute(() -> SpyManager.sendList(player)));

        ServerPlayNetworking.registerGlobalReceiver(SPY_REQUEST, (server, player, handler, buf, sender) -> {
            UUID target = buf.readUuid();
            server.execute(() -> SpyManager.start(player, target));
        });

        ServerPlayNetworking.registerGlobalReceiver(DIVE_TOGGLE, (server, player, handler, buf, sender) ->
                server.execute(() -> ShadowDive.toggle(player)));

        ServerPlayNetworking.registerGlobalReceiver(SNUFF, (server, player, handler, buf, sender) ->
                server.execute(() -> SnuffOut.use(player)));

        ServerPlayNetworking.registerGlobalReceiver(SPY_CANCEL, (server, player, handler, buf, sender) ->
                server.execute(() -> SpyManager.stop(player, true)));

        ServerPlayNetworking.registerGlobalReceiver(CAST, (server, player, handler, buf, sender) -> {
            int book = buf.readVarInt();
            int count = Math.min(buf.readVarInt(), ShadowRitual.STEPS);
            List<String> drawn = new ArrayList<>(count);
            for (int i = 0; i < count; i++) drawn.add(buf.readString(RitualPatterns.MAX_LENGTH));
            server.execute(() -> RitualCasting.cast(player, book, drawn));
        });

        ServerPlayNetworking.registerGlobalReceiver(CAST_POSE, (server, player, handler, buf, sender) -> {
            boolean on = buf.readBoolean();
            server.execute(() -> CastPoses.set(player, on));
        });
    }
}
