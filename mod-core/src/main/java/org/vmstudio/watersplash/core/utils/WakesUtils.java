package org.vmstudio.watersplash.core.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.vmstudio.watersplash.core.config.WakesConfig;
import org.vmstudio.watersplash.core.config.enums.EffectSpawningRule;
import org.vmstudio.watersplash.core.simulation.WakeHandler;
import org.vmstudio.watersplash.core.simulation.WakeNode;

import java.util.ArrayList;

public class WakesUtils {

    public static int getLightColor(Level world, BlockPos blockPos) {
        return LevelRenderer.getLightColor(world, blockPos);
    }

    public static void placeFallSplash(Entity entity) {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;

        for (WakeNode node : WakeNode.Factory.splashNodes(entity, (int) Math.floor(entity.getY()))) {
            wakeHandler.insert(node);
        }
    }

    public static void placeWakeTrail(Entity entity) {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;

        double velocity = entity.getDeltaMovement().horizontalDistance();
        int y = (int) Math.floor(entity.getY());

        Vec3 prevPos = entity.position().subtract(entity.getDeltaMovement());
        if (prevPos == null) {
            return;
        }
        for (WakeNode node : WakeNode.Factory.thickNodeTrail(prevPos.x, prevPos.z, entity.getX(), entity.getZ(), y, WakesConfig.initialStrength, velocity, entity.getBbWidth())) {
            wakeHandler.insert(node);
        }
    }

    public static EffectSpawningRule getEffectRuleFromSource(Entity source) {
        if (source instanceof Boat) {
            return WakesConfig.boatSpawning;
        }
        if (source instanceof Player player) {
            if (player.isSpectator()) {
                return EffectSpawningRule.DISABLED;
            }
            if (player == Minecraft.getInstance().player) {
                return WakesConfig.playerSpawning;
            }
            return WakesConfig.otherPlayersSpawning;
        }
        if (source instanceof LivingEntity) {
            return WakesConfig.mobSpawning;
        }
        if (source instanceof ItemEntity) {
            return WakesConfig.itemSpawning;
        }
        return EffectSpawningRule.DISABLED;
    }

    public static void bresenhamLine(int x1, int y1, int x2, int y2, ArrayList<Long> points) {
        int dy = y2 - y1;
        int dx = x2 - x1;
        if (dx == 0) {
            if (y2 < y1) {
                int temp = y1;
                y1 = y2;
                y2 = temp;
            }
            for (int y = y1; y < y2 + 1; y++) {
                points.add(posAsLong(x1, y));
            }
        } else {
            float k = (float) dy / dx;
            int adjust = k >= 0 ? 1 : -1;
            int offset = 0;
            if (k <= 1 && k >= -1) {
                int delta = Math.abs(dy) * 2;
                int threshold = Math.abs(dx);
                int thresholdInc = Math.abs(dx) * 2;
                int y = y1;
                if (x2 < x1) {
                    int temp = x1;
                    x1 = x2;
                    x2 = temp;
                    y = y2;
                }
                for (int x = x1; x < x2 + 1; x++) {
                    points.add(posAsLong(x, y));
                    offset += delta;
                    if (offset >= threshold) {
                        y += adjust;
                        threshold += thresholdInc;
                    }
                }
            } else {
                int delta = Math.abs(dx) * 2;
                int threshold = Math.abs(dy);
                int thresholdInc = Math.abs(dy) * 2;
                int x = x1;
                if (y2 < y1) {
                    int temp = y1;
                    y1 = y2;
                    y2 = temp;
                }
                for (int y = y1; y < y2 + 1; y++) {
                    points.add(posAsLong(x, y));
                    offset += delta;
                    if (offset >= threshold) {
                        x += adjust;
                        threshold += thresholdInc;
                    }
                }
            }
        }
    }

    public static long posAsLong(int x, int y) {
        int xs = x >> 31 & 1;
        int ys = y >> 31 & 1;
        x &= ~(1 << 31);
        y &= ~(1 << 31);
        long pos = (long) x << 32 | y;
        pos ^= (-xs ^ pos) & (1L << 63);
        pos ^= (-ys ^ pos) & (1L << 31);
        return pos;
    }

    public static int[] longAsPos(long pos) {
        return new int[] {(int) (pos >> 32), (int) pos};
    }

    public static float getFluidLevel(Level world, Entity entityInFluid) {
        AABB box = entityInFluid.getBoundingBox();
        return getFluidLevel(world,
                (int) box.minX, (int) box.maxX,
                (int) box.minY, (int) box.maxY,
                (int) box.minZ, (int) box.maxZ);
    }

    private static float getFluidLevel(Level world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        yLoop:
        for (int y = minY; y < maxY; ++y) {
            float f = 0.0f;
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    blockPos.set(x, y, z);
                    var fluidState = world.getFluidState(blockPos);
                    if (fluidState.isSource()) {
                        f = Math.max(f, fluidState.getHeight(world, blockPos));
                    }
                    if (f >= 1.0f) continue yLoop;
                }
            }
            if (!(f < 1.0f)) continue;
            return blockPos.getY() + f;
        }
        return maxY + 1;
    }
}
