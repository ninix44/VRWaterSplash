package me.phoenixra.visorexample.core.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class WaterSplashHandler {

    private Vec3 lastHandPos = Vec3.ZERO;

    public WaterSplashHandler() {
        ClientTickEvent.CLIENT_POST.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        // testing hand position (без vr очков и контроллеров)
        // todo заменить на позиции рук из VisorAPI (if/else for testing)

        //VisorAPI.client().getVRLocalPlayer()

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getLookAngle();
        Vec3 currentHandPos = eyePos.add(lookVec.scale(1.5));

        double speed = currentHandPos.distanceTo(lastHandPos);

        BlockPos pos = BlockPos.containing(currentHandPos.x, currentHandPos.y, currentHandPos.z);
        boolean isWater = mc.level.getBlockState(pos).is(Blocks.WATER);

        boolean isSurface = mc.level.getBlockState(pos.above()).isAir();

        // boolean isSurface = !mc.level.getBlockState(pos.above()).is(Blocks.WATER);

        if (isWater && isSurface && speed > 0.05) {

            for (int i = 0; i < 10; i++) {
                mc.level.addParticle(
                    ParticleTypes.SPLASH,
                    currentHandPos.x + (Math.random() - 0.5) * 0.3,
                    Math.floor(currentHandPos.y) + 0.9,
                    currentHandPos.z + (Math.random() - 0.5) * 0.3,
                    0, 0.2, 0
                );
            }

            if (speed > 0.1) {
                mc.level.playLocalSound(
                    currentHandPos.x, currentHandPos.y, currentHandPos.z,
                    SoundEvents.PLAYER_SPLASH,
                    SoundSource.PLAYERS,
                    0.4f,
                    1.0f + (float)(Math.random() * 0.4),
                    false
                );
            }

            // todo haptic, вибрация для контроллеров, у меня пока нету, нужно будет также подключить VisorAPI
            // VisorAPI.haptics().trigger(Hand.RIGHT, 0.1f, 1.0f); // +-
        }

        lastHandPos = currentHandPos;
    }
}
