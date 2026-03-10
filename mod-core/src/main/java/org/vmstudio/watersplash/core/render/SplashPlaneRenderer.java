package org.vmstudio.watersplash.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.vmstudio.watersplash.core.config.WakesConfig;

public class SplashPlaneRenderer {

    public static void initSplashPlane() {
    }

    public static void render(PoseStack matrices, double x, double y, double z, float yaw, float velocity) {
        if (WakesConfig.disableMod || !WakesConfig.spawnParticles) {
            return;
        }

        if (velocity < WakesConfig.maxSplashPlaneVelocity * 0.3f) {
            return;
        }

        float progress = Math.min(1f, velocity / WakesConfig.maxSplashPlaneVelocity);
        float scalar = (float) (WakesConfig.splashPlaneScale * Math.sqrt(1 + progress));

        matrices.pushPose();
        matrices.translate(x, y, z);

        matrices.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(yaw + 180)));
        matrices.scale(scalar, scalar, scalar);

        matrices.popPose();
    }
}
