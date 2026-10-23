package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.Shadowbound;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class FxQuality {
    public static final int LOW = 0, MEDIUM = 1, HIGH = 2;

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("shadowbound-client.properties");

    private static int forced = -1;
    private static int level = MEDIUM;
    private static Vec3d cam = Vec3d.ZERO;

    private FxQuality() {}

    public static void load() {
        Properties props = new Properties();
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE)) {
                props.load(r);
            } catch (IOException e) {
                Shadowbound.LOGGER.warn("couldn't read {}", FILE, e);
            }
        }
        String v = props.getProperty("effects", "auto").trim().toLowerCase(Locale.ROOT);
        forced = switch (v) {
            case "low" -> LOW;
            case "medium" -> MEDIUM;
            case "high" -> HIGH;
            default -> -1;
        };
        if (!Files.exists(FILE)) {
            try (Writer w = Files.newBufferedWriter(FILE)) {
                w.write("# auto follows Video Settings > Graphics (Fast = low, Fancy = medium, Fabulous = high)\n");
                w.write("# low, medium or high to pick one yourself\n");
                w.write("effects=auto\n");
            } catch (IOException e) {
                Shadowbound.LOGGER.warn("couldn't write {}", FILE, e);
            }
        }
    }

    public static void update(Vec3d cameraPos) {
        cam = cameraPos;
        if (forced >= 0) {
            level = forced;
            return;
        }
        GraphicsMode g = MinecraftClient.getInstance().options.getGraphicsMode().getValue();
        level = switch (g) {
            case FAST -> LOW;
            case FANCY -> MEDIUM;
            case FABULOUS -> HIGH;
        };
    }

    public static int level() {
        return level;
    }

    public static boolean low() {
        return level == LOW;
    }

    public static double distSq(double x, double y, double z) {
        double dx = x - cam.x, dy = y - cam.y, dz = z - cam.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public static double distSq(Entity e) {
        return distSq(e.getX(), e.getY(), e.getZ());
    }

    public static int shells(double distSq) {
        int max = level == HIGH ? 4 : level == MEDIUM ? 3 : 1;
        if (distSq > 48 * 48) return 0;
        if (distSq > 24 * 24) return Math.min(max, 1);
        if (distSq > 12 * 12) return Math.min(max, 2);
        return max;
    }

    public static float density(double distSq) {
        float base = level == HIGH ? 1f : level == MEDIUM ? 0.7f : 0.4f;
        if (distSq > 40 * 40) return base * 0.35f;
        if (distSq > 20 * 20) return base * 0.6f;
        return base;
    }
}
