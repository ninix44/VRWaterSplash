package me.phoenixra.visorexample.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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

    private Vec3 lastMainHandPos = null;
    private Vec3 lastOffHandPos = null;
    private Vec3 lastDesktopPos = null;

    public WaterSplashHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        Vec3 currentDesktop = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(1.5));
        if (lastDesktopPos != null) {
            checkSplash(mc, currentDesktop, lastDesktopPos, null);
        }
        lastDesktopPos = currentDesktop;

        VRLocalPlayer vrPlayer = VisorAPI.client().getVRLocalPlayer();
        if (vrPlayer != null && VisorAPI.clientState().playMode().canPlayVR()) {
            PlayerPoseClient pose = vrPlayer.getPoseData(PlayerPoseType.RENDER);

            Vec3 currentMain = toVec3(pose.getMainHand().getPosition());
            if (lastMainHandPos != null) {
                checkSplash(mc, currentMain, lastMainHandPos, HandType.MAIN);
            }
            lastMainHandPos = currentMain;

            Vec3 currentOff = toVec3(pose.getOffhand().getPosition());
            if (lastOffHandPos != null) {
                checkSplash(mc, currentOff, lastOffHandPos, HandType.OFFHAND);
            }
            lastOffHandPos = currentOff;
        }
    }

    private void checkSplash(Minecraft mc, Vec3 current, Vec3 last, HandType hand) {
        double speed = current.distanceTo(last);
        if (speed < 0.05) return;

        BlockPos pos = BlockPos.containing(current.x, current.y, current.z);
        if (mc.level.getBlockState(pos).is(Blocks.WATER) && mc.level.getBlockState(pos.above()).isAir()) {
            spawnSplash(mc, current, speed, hand);
        }
    }


    // todo Если рука чуть-чуть входит в воду, то сразу плесканье сделать
    // todo сделать волны, будет зависеть от удара по воде через GLSL Shader
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

            // todo поиграться с вибрацией phoenixra
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
