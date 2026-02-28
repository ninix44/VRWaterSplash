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

    private Vec3 lastMainHandPos = null;
    private Vec3 lastOffHandPos = null;
    private Vec3 lastDesktopPos = null;

    public WaterSplashHandler() {
        ClientTickEvent.CLIENT_POST.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        VRLocalPlayer vrPlayer = VisorAPI.client().getVRLocalPlayer();

        if (vrPlayer != null && VisorAPI.clientState().playMode().canPlayVR()) {
            PlayerPoseClient pose = vrPlayer.getPoseData(PlayerPoseType.RENDER);

            Vec3 currentMain = toVec3(pose.getMainHand().getPosition());
            if (lastMainHandPos != null) {
                processHandSplash(mc, currentMain, lastMainHandPos, HandType.MAIN);
            }
            lastMainHandPos = currentMain;

            Vec3 currentOff = toVec3(pose.getOffhand().getPosition());
            if (lastOffHandPos != null) {
                processHandSplash(mc, currentOff, lastOffHandPos, HandType.OFFHAND);
            }
            lastOffHandPos = currentOff;

        } else {
            Vec3 currentPos = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(1.5));
            if (lastDesktopPos != null) {
                processHandSplash(mc, currentPos, lastDesktopPos, null);
            }
            lastDesktopPos = currentPos;
        }
    }

    private void processHandSplash(Minecraft mc, Vec3 pos, Vec3 lastPos, HandType handType) {
        double speed = pos.distanceTo(lastPos);
        BlockPos bPos = BlockPos.containing(pos.x, pos.y, pos.z);

        boolean isInWater = mc.level.getBlockState(bPos).is(Blocks.WATER);
        boolean isSurface = mc.level.getBlockState(bPos.above()).isAir();

        if (isInWater && isSurface && speed > 0.05) {

            for (int i = 0; i < 6; i++) {
                mc.level.addParticle(
                    ParticleTypes.SPLASH,
                    pos.x + (Math.random() - 0.5) * 0.2,
                    Math.floor(pos.y) + 0.95,
                    pos.z + (Math.random() - 0.5) * 0.2,
                    0, 0.1, 0
                );
            }

            if (speed > 0.1) {
                mc.level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS,
                    0.3f, 1.0f + (float)Math.random() * 0.4f, false);

                // haptics testing on phoenixra
                if (handType != null) {
                    float amplitude = (float) Math.min(speed * 3.0, 0.8);
                    float frequency = 160.0f;
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
    }

    private Vec3 toVec3(Vector3fc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
