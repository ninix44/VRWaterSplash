package me.phoenixra.visorexample.core.client;

import dev.architectury.event.events.client.ClientTickEvent;
import me.phoenixra.visor.api.VisorAPI;
import me.phoenixra.visor.api.client.player.VRLocalPlayer;
import me.phoenixra.visor.api.client.player.pose.PlayerPoseClient;
import me.phoenixra.visor.api.client.player.pose.PlayerPoseType;
import me.phoenixra.visor.api.common.HandType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public class WaterSplashHandler {

    private Vec3 lastMainHandPos = Vec3.ZERO;
    private Vec3 lastOffHandPos = Vec3.ZERO;
    private Vec3 lastDesktopPos = Vec3.ZERO;

    public WaterSplashHandler() {
        ClientTickEvent.CLIENT_POST.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        // Мышка работает ВСЕГДА для тестов (потом убрать, если нужно будет)
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getLookAngle();
        Vec3 currentDesktopHandPos = eyePos.add(lookVec.scale(1.5));

        if (lastDesktopPos != Vec3.ZERO) {
            double speed = currentDesktopHandPos.distanceTo(lastDesktopPos);

            BlockPos pos = BlockPos.containing(currentDesktopHandPos.x, currentDesktopHandPos.y, currentDesktopHandPos.z);
            boolean isWater = mc.level.getBlockState(pos).is(Blocks.WATER);
            boolean isSurface = mc.level.getBlockState(pos.above()).isAir();

            if (isWater && isSurface && speed > 0.05) {
                spawnSplash(mc, currentDesktopHandPos, speed, null);
            }
        }
        lastDesktopPos = currentDesktopHandPos;

        VRLocalPlayer vrPlayer = VisorAPI.client().getVRLocalPlayer();

        if (vrPlayer != null && VisorAPI.clientState().playMode().canPlayVR()) {
            PlayerPoseClient pose = vrPlayer.getPoseData(PlayerPoseType.RENDER);

            Vec3 currentMain = toVec3(pose.getMainHand().getPosition());
            if (lastMainHandPos != Vec3.ZERO) {
                double speed = currentMain.distanceTo(lastMainHandPos);
                BlockPos pos = BlockPos.containing(currentMain.x, currentMain.y, currentMain.z);
                if (mc.level.getBlockState(pos).is(Blocks.WATER) && mc.level.getBlockState(pos.above()).isAir() && speed > 0.05) {
                    spawnSplash(mc, currentMain, speed, HandType.MAIN);
                }
            }
            lastMainHandPos = currentMain;

            Vec3 currentOff = toVec3(pose.getOffhand().getPosition());
            if (lastOffHandPos != Vec3.ZERO) {
                double speed = currentOff.distanceTo(lastOffHandPos);
                BlockPos pos = BlockPos.containing(currentOff.x, currentOff.y, currentOff.z);
                if (mc.level.getBlockState(pos).is(Blocks.WATER) && mc.level.getBlockState(pos.above()).isAir() && speed > 0.05) {
                    spawnSplash(mc, currentOff, speed, HandType.OFFHAND);
                }
            }
            lastOffHandPos = currentOff;
        }
    }

    private void spawnSplash(Minecraft mc, Vec3 pos, double speed, HandType handType) {
        for (int i = 0; i < 10; i++) {
            mc.level.addParticle(
                ParticleTypes.SPLASH,
                pos.x + (Math.random() - 0.5) * 0.3,
                Math.floor(pos.y) + 0.9,
                pos.z + (Math.random() - 0.5) * 0.3,
                0, 0.2, 0
            );
        }

        if (speed > 0.1) {
            mc.level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS,
                0.4f, 1.0f + (float)(Math.random() * 0.4), false);

            if (handType != null) {
                float amplitude = (float) Math.min(speed * 4.0, 0.7);

                float frequency = 120.0f;
                float durationSeconds = 0.05f;

                VisorAPI.client().getInputManager().triggerHapticPulse(
                    handType,
                    frequency,
                    amplitude,
                    durationSeconds
                );
            }
        }
    }

    private Vec3 toVec3(Vector3fc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
