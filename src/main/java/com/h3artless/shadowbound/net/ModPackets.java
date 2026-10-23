package com.h3artless.shadowbound.net;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.dive.ShadowDive;
import com.h3artless.shadowbound.snuff.SnuffOut;
import com.h3artless.shadowbound.spy.SpyManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

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
    }
}
