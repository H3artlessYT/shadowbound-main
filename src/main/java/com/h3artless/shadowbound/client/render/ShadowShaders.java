package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.Shadowbound;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.io.IOException;

public final class ShadowShaders {
    public static ShaderProgram sigil;
    public static ShaderProgram sprite;
    public static ShaderProgram spriteAlpha;
    public static ShaderProgram orb;
    public static ShaderProgram smoke;
    public static ShaderProgram wisp;
    public static ShaderProgram puddle;
    public static ShaderProgram trail;
    public static ShaderProgram puff;
    public static ShaderProgram pylonStone;
    public static ShaderProgram pylonCrystal;
    public static ShaderProgram pylonRune;
    public static ShaderProgram pylonColumn;
    public static ShaderProgram book;
    public static ShaderProgram inkStroke;
    public static ShaderProgram inkDot;
    public static ShaderProgram airStroke;
    public static ShaderProgram airGlyph;
    public static ShaderProgram airSigil;
    public static ShaderProgram cooldownSeal;
    public static ShaderProgram soulHollow;
    public static ShaderProgram soulTrail;
    public static ShaderProgram soulFace;
    public static ShaderProgram chestPortal;
    public static ShaderProgram laser;
    public static ShaderProgram rotorMetal;
    public static ShaderProgram rotorGlass;
    public static ShaderProgram rotorCore;
    public static ShaderProgram voidHolo;
    public static ShaderProgram voidSky;
    public static ShaderProgram voidShade;
    public static ShaderProgram voidBolt;
    public static ShaderProgram extStone;
    public static ShaderProgram voidEye;
    public static ShaderProgram pedestalSigil;
    public static ShaderProgram pedestalGlow;
    public static ShaderProgram pedestalSmoke;
    public static ShaderProgram pedestalPortal;
    public static ShaderProgram pedestalFunnel;
    public static ShaderProgram portalSky;

    private ShadowShaders() {}

    public static void register(CoreShaderRegistrationCallback.RegistrationContext ctx) throws IOException {
        ctx.register(Shadowbound.id("ritual_sigil"), VertexFormats.POSITION_TEXTURE_COLOR, p -> sigil = p);
        ctx.register(Shadowbound.id("ritual_sprite"), VertexFormats.POSITION_TEXTURE_COLOR, p -> sprite = p);
        ctx.register(Shadowbound.id("ritual_sprite_alpha"), VertexFormats.POSITION_TEXTURE_COLOR, p -> spriteAlpha = p);
        ctx.register(Shadowbound.id("ritual_orb"), VertexFormats.POSITION_TEXTURE_COLOR, p -> orb = p);
        ctx.register(Shadowbound.id("shadow_smoke"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, p -> smoke = p);
        ctx.register(Shadowbound.id("shadow_wisp"), VertexFormats.POSITION_TEXTURE_COLOR, p -> wisp = p);
        ctx.register(Shadowbound.id("shadow_puddle"), VertexFormats.POSITION_TEXTURE_COLOR, p -> puddle = p);
        ctx.register(Shadowbound.id("shadow_trail"), VertexFormats.POSITION_TEXTURE_COLOR, p -> trail = p);
        ctx.register(Shadowbound.id("shadow_puff"), VertexFormats.POSITION_TEXTURE_COLOR, p -> puff = p);
        ctx.register(Shadowbound.id("soul_pylon_stone"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, p -> pylonStone = p);
        ctx.register(Shadowbound.id("soul_pylon_crystal"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, p -> pylonCrystal = p);
        ctx.register(Shadowbound.id("soul_pylon_rune"), VertexFormats.POSITION_TEXTURE_COLOR, p -> pylonRune = p);
        ctx.register(Shadowbound.id("soul_pylon_column"), VertexFormats.POSITION_TEXTURE_COLOR, p -> pylonColumn = p);
        ctx.register(Shadowbound.id("shadow_book"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, p -> book = p);
        ctx.register(Shadowbound.id("ink_stroke"), VertexFormats.POSITION_TEXTURE_COLOR, p -> inkStroke = p);
        ctx.register(Shadowbound.id("ink_dot"), VertexFormats.POSITION_TEXTURE_COLOR, p -> inkDot = p);
        ctx.register(Shadowbound.id("air_stroke"), VertexFormats.POSITION_TEXTURE_COLOR, p -> airStroke = p);
        ctx.register(Shadowbound.id("air_glyph"), VertexFormats.POSITION_TEXTURE_COLOR, p -> airGlyph = p);
        ctx.register(Shadowbound.id("air_sigil"), VertexFormats.POSITION_TEXTURE_COLOR, p -> airSigil = p);
        ctx.register(Shadowbound.id("cooldown_seal"), VertexFormats.POSITION_TEXTURE_COLOR, p -> cooldownSeal = p);
        ctx.register(Shadowbound.id("soul_hollow"), VertexFormats.POSITION_TEXTURE_COLOR, p -> soulHollow = p);
        ctx.register(Shadowbound.id("soul_trail"), VertexFormats.POSITION_TEXTURE_COLOR, p -> soulTrail = p);
        ctx.register(Shadowbound.id("soul_face"), VertexFormats.POSITION_TEXTURE_COLOR, p -> soulFace = p);
        ctx.register(Shadowbound.id("chest_portal"), VertexFormats.POSITION_TEXTURE_COLOR, p -> chestPortal = p);
        ctx.register(Shadowbound.id("shadow_laser"), VertexFormats.POSITION_TEXTURE_COLOR, p -> laser = p);
        ctx.register(Shadowbound.id("rotor_metal"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, p -> rotorMetal = p);
        ctx.register(Shadowbound.id("rotor_glass"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, p -> rotorGlass = p);
        ctx.register(Shadowbound.id("rotor_core"), VertexFormats.POSITION_TEXTURE_COLOR, p -> rotorCore = p);
        ctx.register(Shadowbound.id("sb_void_holo"), VertexFormats.POSITION_TEXTURE_COLOR, p -> voidHolo = p);
        ctx.register(Shadowbound.id("sb_void_sky"), VertexFormats.POSITION_TEXTURE_COLOR, p -> voidSky = p);
        ctx.register(Shadowbound.id("sb_void_shade"), VertexFormats.POSITION_TEXTURE_COLOR, p -> voidShade = p);
        ctx.register(Shadowbound.id("sb_void_bolt"), VertexFormats.POSITION_TEXTURE_COLOR, p -> voidBolt = p);
        ctx.register(Shadowbound.id("sb_ext_stone"), VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, p -> extStone = p);
        ctx.register(Shadowbound.id("sb_void_eye"), VertexFormats.POSITION_TEXTURE_COLOR, p -> voidEye = p);
        ctx.register(Shadowbound.id("pedestal_sigil"), VertexFormats.POSITION_TEXTURE_COLOR, p -> pedestalSigil = p);
        ctx.register(Shadowbound.id("pedestal_glow"), VertexFormats.POSITION_TEXTURE_COLOR, p -> pedestalGlow = p);
        ctx.register(Shadowbound.id("pedestal_smoke"), VertexFormats.POSITION_TEXTURE_COLOR, p -> pedestalSmoke = p);
        ctx.register(Shadowbound.id("pedestal_portal"), VertexFormats.POSITION_TEXTURE_COLOR, p -> pedestalPortal = p);
        ctx.register(Shadowbound.id("pedestal_funnel"), VertexFormats.POSITION_TEXTURE_COLOR, p -> pedestalFunnel = p);
        ctx.register(Shadowbound.id("pedestal_portal_sky"), VertexFormats.POSITION_TEXTURE_COLOR, p -> portalSky = p);
    }

    private static final Matrix4f INV_VIEW = new Matrix4f();

    public static void updateSmoke(WorldRenderContext ctx) {
        Camera cam = ctx.camera();
        Vec3d p = cam.getPos();
        FxQuality.update(p);
        float q = FxQuality.level();
        quality(wisp, q);
        quality(trail, q);
        quality(puddle, q);
        quality(smoke, q);

        INV_VIEW.identity()
                .rotateY(-(cam.getYaw() + 180f) * MathHelper.RADIANS_PER_DEGREE)
                .rotateX(-cam.getPitch() * MathHelper.RADIANS_PER_DEGREE);

        if (portalSky != null) {
            GlUniform pr = portalSky.getUniform("InvViewRot");
            if (pr != null) pr.set(INV_VIEW);
        }

        if (smoke == null) return;
        GlUniform rot = smoke.getUniform("InvViewRot");
        if (rot != null) rot.set(INV_VIEW);

        GlUniform off = smoke.getUniform("CameraOffset");
        if (off != null) off.set((float) (p.x % 2048.0), (float) (p.y % 2048.0), (float) (p.z % 2048.0));
    }

    private static void quality(ShaderProgram program, float q) {
        if (program == null) return;
        GlUniform u = program.getUniform("Quality");
        if (u != null) u.set(q);
    }
}
