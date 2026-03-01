package dev.ninix.visor.watersplash.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.player.VRLocalPlayer;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseClient;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.common.HandType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WaterSplashHandler {

    private Vec3 lastMainHandPos = null;
    private Vec3 lastOffHandPos = null;
    private Vec3 lastDesktopPos = null;

    private boolean wasMainInWater = false;
    private boolean wasOffInWater = false;
    private boolean wasDesktopInWater = false;

    private final List<WaveRing> activeWaves = new ArrayList<>();

    public WaterSplashHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private static class WaveRing {
        double x, z;
        double radius;
        double maxRadius;
        double expansionSpeed;
        int particleCount;
        float alpha;
        double yOffset;

        double wavePhase;
        double waveSpeed;
        double waveHeight;

        WaveRing(double x, double z, double yOffset, double speed) {
            this.x = x;
            this.z = z;
            this.yOffset = yOffset;
            this.radius = 0.1;
            this.maxRadius = 1.5 + (speed * 3.0);
            this.expansionSpeed = 0.1 + (speed * 0.06);
            this.particleCount = 12 + (int)(speed * 8);
            this.alpha = 1.0f;

            this.wavePhase = 0;
            this.waveSpeed = 6.0 + speed * 2.0;
            this.waveHeight = 0.2 + (speed * 0.4);
        }

        void tick(double deltaTime) {
            radius += expansionSpeed;
            wavePhase += waveSpeed * deltaTime;
            alpha = (float) Math.max(0, 1.0 - (radius / maxRadius));
        }

        boolean isFinished() {
            return radius >= maxRadius || alpha <= 0.05f;
        }

        double getWaveHeightAt(double distanceFromCenter) {
            double normalizedDist = distanceFromCenter / maxRadius;
            double waveProfile = Math.sin(wavePhase - normalizedDist * 6.0);
            double envelope = Math.exp(-Math.pow(normalizedDist * 3.0 - 1.0, 2) * 2.0);
            return waveProfile * envelope * waveHeight * alpha;
        }
    }

    private double lastTickTime = 0;

    private void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        double currentTime = System.currentTimeMillis() / 1000.0;
        double deltaTime = Math.min(0.1, currentTime - lastTickTime);
        lastTickTime = currentTime;

        updateWaves(mc, deltaTime);

        Vec3 currentDesktop = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(1.5));
        if (lastDesktopPos != null) {
            wasDesktopInWater = checkSplash(mc, currentDesktop, lastDesktopPos, null, wasDesktopInWater);
        }
        lastDesktopPos = currentDesktop;

        VRLocalPlayer vrPlayer = VisorAPI.client().getVRLocalPlayer();
        if (vrPlayer != null && VisorAPI.clientState().playMode().canPlayVR()) {
            PlayerPoseClient pose = vrPlayer.getPoseData(PlayerPoseType.RENDER);

            Vec3 currentMain = toVec3(pose.getMainHand().getPosition());
            if (lastMainHandPos != null) {
                wasMainInWater = checkSplash(mc, currentMain, lastMainHandPos, HandType.MAIN, wasMainInWater);
            }
            lastMainHandPos = currentMain;

            Vec3 currentOff = toVec3(pose.getOffhand().getPosition());
            if (lastOffHandPos != null) {
                wasOffInWater = checkSplash(mc, currentOff, lastOffHandPos, HandType.OFFHAND, wasOffInWater);
            }
            lastOffHandPos = currentOff;
        }
    }

    private void updateWaves(Minecraft mc, double deltaTime) {
        Iterator<WaveRing> iterator = activeWaves.iterator();
        while (iterator.hasNext()) {
            WaveRing wave = iterator.next();
            wave.tick(deltaTime);
            spawnWaveParticles(mc, wave);
            if (wave.isFinished()) {
                iterator.remove();
            }
        }
    }

    private void spawnWaveParticles(Minecraft mc, WaveRing wave) {
        if (wave.alpha <= 0.05f) return;

        for (int i = 0; i < wave.particleCount; i++) {
            double angle = (2 * Math.PI * i) / wave.particleCount;
            double offsetX = Math.cos(angle) * wave.radius;
            double offsetZ = Math.sin(angle) * wave.radius;

            double waveH = wave.getWaveHeightAt(wave.radius);
            double spread = 0.1 * wave.alpha;
            double x = wave.x + offsetX + (Math.random() - 0.5) * spread;
            double z = wave.z + offsetZ + (Math.random() - 0.5) * spread;
            double y = wave.yOffset + Math.max(0, waveH);

            mc.level.addParticle(
                ParticleTypes.BUBBLE,
                x, y, z,
                0, wave.alpha * 0.01, 0
            );

            if (wave.alpha > 0.5 && i % 4 == 0) {
                mc.level.addParticle(
                    ParticleTypes.UNDERWATER,
                    x, y - 0.1, z,
                    0, 0, 0
                );
            }
        }
    }

    private boolean checkSplash(Minecraft mc, Vec3 current, Vec3 last, HandType hand, boolean wasInWater) {
        BlockPos pos = BlockPos.containing(current.x, current.y, current.z);
        boolean isInWater = mc.level.getBlockState(pos).is(Blocks.WATER);

        boolean isSurface = isInWater && mc.level.getBlockState(pos.above()).isAir();
        boolean justEntered = isInWater && !wasInWater;

        Vec3 movement = current.subtract(last);
        double horizontalSpeed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        double totalSpeed = movement.length();

        if (justEntered) {
            if (totalSpeed > 0.01) {
                spawnSplash(mc, current, totalSpeed, hand, true);
            }
        }
        else if (isSurface) {
            if (horizontalSpeed > 0.07) {
                spawnSplash(mc, current, horizontalSpeed, hand, false);
            }
        }

        return isInWater;
    }

    // todo maybe later GLSL shader?
    private void spawnSplash(Minecraft mc, Vec3 pos, double speed, HandType handType, boolean isEntry) {
        int particleCount = isEntry ? (int) Math.min(speed * 50, 15) : (int) Math.min(speed * 40, 10);

        for (int i = 0; i < particleCount; i++) {
            mc.level.addParticle(
                ParticleTypes.SPLASH,
                pos.x + (Math.random() - 0.5) * 0.4,
                Math.floor(pos.y) + 0.95,
                pos.z + (Math.random() - 0.5) * 0.4,
                (Math.random() - 0.5) * 0.1,
                isEntry ? 0.1 + Math.random() * 0.3 : 0.05 + Math.random() * 0.1,
                (Math.random() - 0.5) * 0.1
            );
        }

        if (speed > 0.15) {
            int waveCount = isEntry ? 2 : 1;
            double surfaceY = Math.floor(pos.y) + 0.95;

            for (int w = 0; w < waveCount; w++) {
                double adjustedSpeed = speed * (1.0 - w * 0.15);
                if (adjustedSpeed > 0.1) {
                    WaveRing wave = new WaveRing(pos.x, pos.z, surfaceY, adjustedSpeed);
                    activeWaves.add(wave);
                }
            }
        }

        // todo мб раньше проигрывать звук волны // testing
        if (speed > 0.08 || isEntry) {
            float volume = isEntry ? (float) Math.min(speed * 2.5, 0.6) : (float) Math.min(speed * 1.5, 0.4);
            mc.level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS,
                volume, 1.0f + (float)(Math.random() * 0.5), false);

            // todo добавить в ГУИ с другими аддонами настройку "регулятор" вибрации, чтобы настроить до идеала, а потом уже в коде поменять
            if (handType != null) {
                float amplitude = isEntry ? (float) Math.min(speed * 4.0, 0.8) : (float) Math.min(speed * 2.5, 0.5);

                float frequency = isEntry ? 120.0f : 70.0f;
                float duration = 0.05f;

                VisorAPI.client().getInputManager().triggerHapticPulse(
                    handType,
                    frequency,
                    amplitude,
                    duration
                );
            }
        }
    }

    private Vec3 toVec3(Vector3fc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
