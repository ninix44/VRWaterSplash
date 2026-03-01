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

public class WaterSplashHandler {

    private Vec3 lastMainHandPos = null;
    private Vec3 lastOffHandPos = null;
    private Vec3 lastDesktopPos = null;

    private boolean wasMainInWater = false;
    private boolean wasOffInWater = false;
    private boolean wasDesktopInWater = false;

    public WaterSplashHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

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

    // todo сделать волны, будет зависеть от удара по воде через GLSL Shader
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
