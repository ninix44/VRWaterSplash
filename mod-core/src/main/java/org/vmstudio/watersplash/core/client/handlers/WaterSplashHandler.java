package org.vmstudio.watersplash.core.client.handlers;

import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.player.VRLocalPlayer;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseClient;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.common.HandType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.vmstudio.watersplash.core.config.WakesConfig;
import org.vmstudio.watersplash.core.render.SplashPlaneRenderer;
import org.vmstudio.watersplash.core.render.WakeRenderer;
import org.vmstudio.watersplash.core.simulation.WakeHandler;
import org.vmstudio.watersplash.core.simulation.WakeNode;

import java.util.WeakHashMap;

public class WaterSplashHandler {

    private Vec3 lastMainHandPos = null;
    private Vec3 lastOffHandPos = null;
    private Vec3 lastDesktopPos = null;

    private boolean wasMainInWater = false;
    private boolean wasOffInWater = false;
    private boolean wasDesktopInWater = false;

    private final WeakHashMap<Entity, Boolean> entityInWaterState = new WeakHashMap<>();
    private final WeakHashMap<Entity, Double> entitySpeedBuffer = new WeakHashMap<>();

    private final WakeRenderer wakeRenderer = new WakeRenderer();

    private double lastTickTime = 0;

    public WaterSplashHandler() {
        if (TickHandlerRegistry.registerHandler != null) {
            TickHandlerRegistry.registerHandler.accept(this::onTick);
        }
        WakeHandler.init(Minecraft.getInstance().level);
        SplashPlaneRenderer.initSplashPlane();
    }

    private void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        if (WakeHandler.getInstance().isEmpty()) {
            WakeHandler.init(mc.level);
        }

        double currentTime = System.currentTimeMillis() / 1000.0;
        double deltaTime = Math.min(0.1, currentTime - lastTickTime);
        lastTickTime = currentTime;

        WakeHandler.getInstance().ifPresent(WakeHandler::tick);

        processEntitySplashes(mc);

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
                        spawnSplash(mc, entity.position(), smoothedSpeed, null, true, 0, 0, weight);
                        WakeHandler.getInstance().ifPresent(handler -> {
                            for (WakeNode node : WakeNode.Factory.splashNodes(entity, (int) Math.floor(entity.getY()))) {
                                handler.insert(node);
                            }
                        });
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
        double horizontalSpeed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        double totalSpeed = movement.length();

        float strengthMultiplier = (hand != null) ? WakesConfig.vrSplashStrength : WakesConfig.mouseSplashStrength;
        totalSpeed *= strengthMultiplier;
        horizontalSpeed *= strengthMultiplier;

        double dirX = 0, dirZ = 0;
        if (horizontalSpeed > 0.001) {
            dirX = movement.x / horizontalSpeed;
            dirZ = movement.z / horizontalSpeed;
        }

        if (justEntered && totalSpeed > 0.01) {
            spawnSplash(mc, current, totalSpeed, hand, true, dirX, dirZ);
            createWakeNodes(mc, current, totalSpeed, dirX, dirZ);
        }
        else if (isSurface && horizontalSpeed > WakesConfig.minSpeedForSplash) {
            spawnSplash(mc, current, horizontalSpeed, hand, false, dirX, dirZ);
            createWakeTrail(mc, last, current, horizontalSpeed, dirX, dirZ);
        }

        return isInWater;
    }

    private void createWakeNodes(Minecraft mc, Vec3 pos, double speed, double dirX, double dirZ) {
        if (mc.level == null) return;

        WakeHandler.getInstance().ifPresent(handler -> {
            int y = (int) Math.floor(pos.y);

            for (WakeNode node : WakeNode.Factory.splashNodes(mc.player, y)) {
                handler.insert(node);
            }
        });
    }

    private void createWakeTrail(Minecraft mc, Vec3 from, Vec3 to, double speed, double dirX, double dirZ) {
        if (mc.level == null) return;

        WakeHandler.getInstance().ifPresent(handler -> {
            int y = (int) Math.floor(from.y);

            for (WakeNode node : WakeNode.Factory.thickNodeTrail(
                from.x, from.z, to.x, to.z,
                y,
                WakesConfig.initialStrength,
                speed,
                0.5f
            )) {
                handler.insert(node);
            }
        });
    }

    private void spawnSplash(Minecraft mc, Vec3 pos, double speed, HandType hand, boolean entry, double dx, double dz) {
        spawnSplash(mc, pos, speed, hand, entry, dx, dz, 1.0f);
    }

    private void spawnSplash(Minecraft mc, Vec3 pos, double speed, HandType hand, boolean entry, double dx, double dz, float weight) {
        boolean isStrongHit = speed > 0.3;
        boolean isMediumHit = speed > 0.1 && speed <= 0.3;
        boolean isWeakHit = speed <= 0.1;

        if (isWeakHit && !entry) {
            int count = Math.min(2, (int) (speed * 10));
            for (int i = 0; i < count; i++) {
                addCustomSplashCloud(mc, pos);
            }
            return;
        }

        if (isMediumHit || entry) {
            int count = (int) (Math.min(speed * 30, entry ? 15 : 8) * weight);
            for (int i = 0; i < count; i++) {
                addCustomSplashCloud(mc, pos);
            }
        }

        if (isStrongHit) {
            int count = (int) (Math.min(speed * 50, 25) * weight);
            for (int i = 0; i < count; i++) {
                addCustomSplashCloud(mc, pos);
            }
        }

        if (speed > WakesConfig.minSpeedForSplash || entry) {
            float volume = (float) Math.min(speed * 2.5 * weight, 0.8);
            mc.level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, volume, 0.9f + (float) Math.random() * 0.2f, false);

            if (hand != null) {
                VisorAPI.client().getInputManager().triggerHapticPulse(hand, entry ? 120f : 70f, entry ? 0.8f : 0.5f, 0.05f);
            }
        }
    }

    public void renderWaves(com.mojang.blaze3d.vertex.PoseStack matrices) {
        wakeRenderer.render(matrices);
    }

    protected void addCustomSplashCloud(Minecraft mc, Vec3 pos) {
    }

    private Vec3 toVec3(Vector3fc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
