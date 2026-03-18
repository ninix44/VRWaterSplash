package org.vmstudio.watersplash.core.client.handlers;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

public class WaterSplashHandler {

    private static final double MIN_JUMP_SPEED = 0.15;
    private final List<DelayedSplash> delayedSplashes = new ArrayList<>();
    private final WeakHashMap<Entity, Boolean> entityInWaterState = new WeakHashMap<>();
    private final WeakHashMap<Entity, Double> entitySpeedBuffer = new WeakHashMap<>();

    private double lastPlayerY = 0;
    private boolean wasPlayerInWater = false;
    private Vec3 lastMainHandPos = null;
    private Vec3 lastOffHandPos = null;
    private Vec3 lastDesktopPos = null;
    private boolean wasMainInWater = false;
    private boolean wasOffInWater = false;
    private boolean wasDesktopInWater = false;

    private static class DelayedSplash {
        long spawnTime;
        Vec3 pos;

        DelayedSplash(Vec3 pos) {
            this.spawnTime = System.currentTimeMillis() + 300;
            this.pos = pos;
        }
    }

    public WaterSplashHandler() {
        if (TickHandlerRegistry.registerHandler != null) {
            TickHandlerRegistry.registerHandler.accept(this::onTick);
        }
    }

    private void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        handlePlayerJumpSplash(mc);
        processEntitySplashes(mc);
        processDelayedSplashes(mc);

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

    private void processDelayedSplashes(Minecraft mc) {
        long now = System.currentTimeMillis();
        Iterator<DelayedSplash> iterator = delayedSplashes.iterator();
        while (iterator.hasNext()) {
            DelayedSplash ds = iterator.next();
            if (now >= ds.spawnTime) {
                spawnDelayedParticles(mc, ds.pos);
                iterator.remove();
            }
        }
    }

    private void spawnDelayedParticles(Minecraft mc, Vec3 pos) {
        int count = 20;
        for (int i = 0; i < count; i++) {
            double startY = Math.floor(pos.y) + 1.5;
            double rx = (Math.random() - 0.5) * 1.0;
            double rz = (Math.random() - 0.5) * 1.0;

            double vx = 0;
            double vy = 0.1;
            double vz = 0;

            mc.level.addParticle(ParticleTypes.CLOUD,
                pos.x + rx, startY, pos.z + rz,
                vx, vy, vz);
        }
    }

    private void handlePlayerJumpSplash(Minecraft mc) {
        Player player = mc.player;
        boolean nowInWater = player.isInWater() || mc.level.getBlockState(player.blockPosition()).is(Blocks.WATER);
        double currentY = player.getY();

        double fallSpeed = lastPlayerY - currentY;

        if (nowInWater && !wasPlayerInWater) {
            if (fallSpeed >= MIN_JUMP_SPEED) {
                spawnImmersiveJumpSplash(mc, player.position(), fallSpeed);
                delayedSplashes.add(new DelayedSplash(player.position()));
            }
        }

        wasPlayerInWater = nowInWater;
        lastPlayerY = currentY;
    }

    private void spawnImmersiveJumpSplash(Minecraft mc, Vec3 pos, double fallSpeed) {
        float volume = (float) Math.min(fallSpeed * 3.0, 1.5);
        mc.level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, volume, 0.8f + (float) Math.random() * 0.4f, false);

        if (VisorAPI.clientState().playMode().canPlayVR()) {
            VisorAPI.client().getInputManager().triggerHapticPulse(HandType.MAIN, 200f, 1.0f, 0.15f);
            VisorAPI.client().getInputManager().triggerHapticPulse(HandType.OFFHAND, 200f, 1.0f, 0.15f);
        }
    }

    private void processEntitySplashes(Minecraft mc) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity itemEntity) {
                double currentSpeed = entity.getDeltaMovement().length();
                double smoothedSpeed = (entitySpeedBuffer.getOrDefault(entity, currentSpeed) + currentSpeed) / 2.0;
                entitySpeedBuffer.put(entity, currentSpeed);

                boolean wasInWater = entityInWaterState.getOrDefault(entity, false);
                boolean isInWater = entity.isInWater();
                if (isInWater && !wasInWater) {
                    ItemStack stack = itemEntity.getItem();
                    float weight = 1.0f + (1.0f - (stack.getMaxStackSize() / 64.0f));
                    if (smoothedSpeed * weight > 0.05) {
                        spawnSplash(mc, entity.position(), smoothedSpeed, null, true, weight);
                    }
                }
                entityInWaterState.put(entity, isInWater);
            }
        }
    }

    private boolean checkSplash(Minecraft mc, Vec3 current, Vec3 last, HandType hand, boolean wasInWater) {
        BlockPos pos = BlockPos.containing(current.x, current.y, current.z);
        boolean isInWater = mc.level.getBlockState(pos).is(Blocks.WATER);

        boolean isSurface = isInWater && mc.level.getBlockState(pos.above()).isAir();
        boolean justEntered = isInWater && !wasInWater;

        Vec3 movement = current.subtract(last);
        double totalSpeed = movement.length();
        if (justEntered && totalSpeed > 0.01) spawnSplash(mc, current, totalSpeed, hand, true);
        else if (isSurface && Math.sqrt(movement.x * movement.x + movement.z * movement.z) > 0.07) spawnSplash(mc, current, 0.1, hand, false);
        return isInWater;
    }

    private void spawnSplash(Minecraft mc, Vec3 pos, double speed, HandType hand, boolean entry) {
        spawnSplash(mc, pos, speed, hand, entry, 1.0f);
    }

    private void spawnSplash(Minecraft mc, Vec3 pos, double speed, HandType hand, boolean entry, float weight) {
        int count = (int) (Math.min(speed * 50, entry ? 25 : 15) * weight);
        for (int i = 0; i < count; i++) {
            double spread = 0.4 * weight;
            mc.level.addParticle(ParticleTypes.SPLASH, pos.x + (Math.random() - 0.5) * spread, Math.floor(pos.y) + 0.95, pos.z + (Math.random() - 0.5) * spread, (Math.random() - 0.5) * 0.1 * weight, (entry ? 0.15 : 0.08) * weight, (Math.random() - 0.5) * 0.1 * weight);
        }
        if (speed > 0.08 || entry) {
            float volume = (float) Math.min(speed * 2.5 * weight, 0.8);
            mc.level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, volume, 0.9f + (float) Math.random() * 0.2f, false);
            if (hand != null && VisorAPI.clientState().playMode().canPlayVR()) VisorAPI.client().getInputManager().triggerHapticPulse(hand, entry ? 120f : 70f, entry ? 0.8f : 0.5f, 0.05f);
        }
    }

    private Vec3 toVec3(Vector3fc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
