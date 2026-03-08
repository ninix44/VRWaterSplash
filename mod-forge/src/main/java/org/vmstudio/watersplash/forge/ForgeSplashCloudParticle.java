package org.vmstudio.watersplash.forge;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class ForgeSplashCloudParticle extends TextureSheetParticle {
    private final boolean isFromPaddles;
    private final SpriteSet sprites;

    protected ForgeSplashCloudParticle(ClientLevel world, double x, double y, double z, SpriteSet sprites, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z);
        this.sprites = sprites;


        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;

        this.xo = x;
        this.yo = y;
        this.zo = z;

        this.isFromPaddles = velocityX == 0 && velocityY == 0 && velocityZ == 0;
        this.lifetime = this.isFromPaddles ? 40 : 20;

        this.setSprite(sprites.get(world.random));
        this.quadSize = isFromPaddles ? 1.0f : 0.5f;
        this.alpha = 1.0f;
    }

    @Override
    public void tick() {
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        if (this.age % 5 == 0) {
            this.setSprite(this.sprites.get(this.level.random));
        }

        this.alpha = 1f - (float) this.age / this.lifetime;

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (level.getFluidState(new BlockPos((int) this.x, (int) this.y, (int) this.z)).is(Fluids.WATER)) {
            this.yd = 0.05;
            this.xd *= 0.95;
            this.yd *= 0.95;
            this.zd *= 0.95;
        } else {
            this.yd -= 0.02;
            this.xd *= 0.98;
            this.yd *= 0.98;
            this.zd *= 0.98;
        }

        this.x += xd;
        this.y += yd;
        this.z += zd;
        this.setPos(this.x, this.y, this.z);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z, double vx, double vy, double vz) {
            return new ForgeSplashCloudParticle(world, x, y, z, this.sprites, vx, vy, vz);
        }
    }
}
