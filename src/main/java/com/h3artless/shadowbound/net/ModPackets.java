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
import com.h3artless.shadowbound.item.ShadowCodexItem;
import com.h3artless.shadowbound.box.ShadowBoxes;
import net.minecraft.util.Hand;
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
    public static final Identifier CONFIG_SYNC = Shadowbound.id("config_sync");
    public static final Identifier CAST_POSE = Shadowbound.id("cast_pose");
    public static final Identifier CAST_POSE_SYNC = Shadowbound.id("cast_pose_sync");
    public static final Identifier SHADOW_DISMISS = Shadowbound.id("shadow_dismiss");
    public static final Identifier SATCHEL_OPEN = Shadowbound.id("satchel_open");
    public static final Identifier CODEX_TEAR = Shadowbound.id("codex_tear");
    public static final Identifier CONSOLE_OPEN = Shadowbound.id("console_open");
    public static final Identifier CONSOLE_ACTION = Shadowbound.id("console_action");
    public static final int COURSE = 0, SAVE = 1, DELETE = 2, LOAD = 3, POUR = 4, REFRESH = 5;

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

        ServerPlayNetworking.registerGlobalReceiver(SHADOW_DISMISS, (server, player, handler, buf, sender) ->
                server.execute(() -> com.h3artless.shadowbound.clone.CloneManager.dismiss(player)));


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

        ServerPlayNetworking.registerGlobalReceiver(CODEX_TEAR, (server, player, handler, buf, sender) -> {
            Hand hand = buf.readBoolean() ? Hand.OFF_HAND : Hand.MAIN_HAND;
            String key = buf.readString(32);
            int step = buf.readVarInt();
            server.execute(() -> {
                ShadowRitual rite = ShadowRitual.byKey(key);
                if (rite != null) ShadowCodexItem.tearOut(player, hand, rite, step);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CONSOLE_ACTION, (server, player, handler, buf, sender) -> {
            int action = buf.readVarInt();
            switch (action) {
                case COURSE -> {
                    String dim = buf.readString(128);
                    int x = buf.readInt(), y = buf.readInt(), z = buf.readInt();
                    server.execute(() -> ShadowBoxes.setCourse(player, dim, x, y, z));
                }
                case SAVE -> {
                    String name = buf.readString(32);
                    server.execute(() -> ShadowBoxes.save(player, name));
                }
                case DELETE, LOAD -> {
                    int index = buf.readVarInt();
                    server.execute(() -> {
                        if (action == DELETE) ShadowBoxes.delete(player, index);
                        else ShadowBoxes.load(player, index);
                    });
                }
                case POUR -> {
                    boolean all = buf.readBoolean();
                    server.execute(() -> ShadowBoxes.pourFromScreen(player, all));
                }
                case REFRESH -> {
                    int screen = buf.readVarInt();
                    server.execute(() -> ShadowBoxes.refresh(player, screen));
                }
                default -> {}
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(CAST_POSE, (server, player, handler, buf, sender) -> {
            boolean on = buf.readBoolean();
            server.execute(() -> CastPoses.set(player, on));
        });
    }
}
