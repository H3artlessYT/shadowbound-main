package com.h3artless.shadowbound.client.particle;

import com.h3artless.shadowbound.registry.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

public final class ShadowParticles {
    private static final int FULL_BRIGHT = 0xF000F0;

    private ShadowParticles() {}

    public static void init() {
        ParticleFactoryRegistry.getInstance().register(ModParticles.RUNE, Rune.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.ASH, Ash.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.MOTE, Mote.Factory::new);
    }

    static final class Rune extends SpriteBillboardParticle {
        private final double cx, cz;
        private final float radius, spin, base;
        private float ang;

        Rune(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
            super(world, x, y, z);
            setSprite(sprites.getSprite(random));
            cx = x;
            cz = z;
            radius = 0.15f + random.nextFloat() * 0.25f;
            ang = random.nextFloat() * MathHelper.TAU;
            spin = (random.nextBoolean() ? 1f : -1f) * (0.08f + random.nextFloat() * 0.07f);
            velocityY = 0.025 + random.nextFloat() * 0.02 + vy;
            maxAge = 34 + random.nextInt(18);
            base = 0.07f + random.nextFloat() * 0.04f;
            scale = base;
            gravityStrength = 0f;
            collidesWithWorld = false;
            float heat = random.nextFloat();
            setColor(0.95f, 0.08f + 0.25f * heat, 0.1f + 0.15f * heat);
            alpha = 0f;
        }

        @Override
        public void tick() {
            prevPosX = x;
            prevPosY = y;
            prevPosZ = z;
            if (age++ >= maxAge) {
                markDead();
                return;
            }
            float k = age / (float) maxAge;
            ang += spin;
            float r = radius * (1f + k * 1.4f);
            setPos(cx + MathHelper.cos(ang) * r, y + velocityY, cz + MathHelper.sin(ang) * r);
            velocityY *= 0.985;
            alpha = MathHelper.clamp(Math.min(k * 6f, (1f - k) * 2.5f), 0f, 1f) * (0.75f + 0.25f * MathHelper.sin(age * 0.9f));
            scale = base * (0.6f + 0.8f * MathHelper.sin(k * MathHelper.PI));
        }

        @Override
        public int getBrightness(float tint) {
            return FULL_BRIGHT;
        }

        @Override
        public ParticleTextureSheet getType() {
            return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
        }

        record Factory(SpriteProvider sprites) implements ParticleFactory<DefaultParticleType> {
            @Override
            public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
                return new Rune(world, x, y, z, vx, vy, vz, sprites);
            }
        }
    }

    static final class Ash extends SpriteBillboardParticle {
        private final float sway, swaySpeed, roll;

        Ash(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
            super(world, x, y, z, vx, vy, vz);
            setSprite(sprites.getSprite(random));
            velocityX = vx + (random.nextFloat() - 0.5f) * 0.01f;
            velocityY = vy + 0.01 + random.nextFloat() * 0.015;
            velocityZ = vz + (random.nextFloat() - 0.5f) * 0.01f;
            maxAge = 50 + random.nextInt(40);
            scale = 0.05f + random.nextFloat() * 0.04f;
            gravityStrength = 0.004f;
            velocityMultiplier = 0.96f;
            sway = 0.006f + random.nextFloat() * 0.006f;
            swaySpeed = 0.15f + random.nextFloat() * 0.1f;
            roll = (random.nextFloat() - 0.5f) * 0.25f;
            angle = random.nextFloat() * MathHelper.TAU;
            setColor(1f, 1f, 1f);
        }

        @Override
        public void tick() {
            super.tick();
            prevAngle = angle;
            angle += roll;
            velocityX += MathHelper.sin(age * swaySpeed) * sway;
            velocityZ += MathHelper.cos(age * swaySpeed * 0.8f) * sway;
            float k = age / (float) maxAge;
            alpha = MathHelper.clamp((1f - k) * 2f, 0f, 1f);
        }

        @Override
        public int getBrightness(float tint) {
            int base = super.getBrightness(tint);
            int block = Math.max(base & 0xFF, 160);
            return (base & 0xFF0000) | block;
        }

        @Override
        public ParticleTextureSheet getType() {
            return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
        }

        record Factory(SpriteProvider sprites) implements ParticleFactory<DefaultParticleType> {
            @Override
            public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
                return new Ash(world, x, y, z, vx, vy, vz, sprites);
            }
        }
    }

    static final class Mote extends SpriteBillboardParticle {
        private final SpriteProvider sprites;
        private final double cx, cy, cz;
        private final float radius, speed, rise;
        private float ang;

        Mote(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
            super(world, x, y, z);
            this.sprites = sprites;
            cx = x;
            cy = y;
            cz = z;
            radius = (float) vx > 0 ? (float) vx : 0.45f + random.nextFloat() * 0.25f;
            ang = (float) vz != 0 ? (float) vz : random.nextFloat() * MathHelper.TAU;
            speed = 0.07f + random.nextFloat() * 0.05f;
            rise = 0.006f + random.nextFloat() * 0.01f + (float) vy;
            maxAge = 40 + random.nextInt(30);
            scale = 0.035f + random.nextFloat() * 0.025f;
            gravityStrength = 0f;
            collidesWithWorld = false;
            float heat = random.nextFloat();
            setColor(1f, 0.15f + 0.45f * heat, 0.1f + 0.35f * heat);
            setSpriteForAge(sprites);
        }

        @Override
        public void tick() {
            prevPosX = x;
            prevPosY = y;
            prevPosZ = z;
            if (age++ >= maxAge) {
                markDead();
                return;
            }
            ang += speed;
            float k = age / (float) maxAge;
            float r = radius * (1f - 0.35f * k);
            setPos(cx + MathHelper.cos(ang) * r, cy + rise * age + MathHelper.sin(age * 0.2f) * 0.04f, cz + MathHelper.sin(ang) * r);
            alpha = MathHelper.clamp(Math.min(k * 5f, (1f - k) * 3f), 0f, 1f);
            setSpriteForAge(sprites);
        }

        @Override
        public int getBrightness(float tint) {
            return FULL_BRIGHT;
        }

        @Override
        public ParticleTextureSheet getType() {
            return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
        }

        record Factory(SpriteProvider sprites) implements ParticleFactory<DefaultParticleType> {
            @Override
            public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
                return new Mote(world, x, y, z, vx, vy, vz, sprites);
            }
        }
    }
}
