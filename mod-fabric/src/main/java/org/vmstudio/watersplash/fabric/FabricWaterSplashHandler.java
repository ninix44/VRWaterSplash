package org.vmstudio.watersplash.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.vmstudio.watersplash.core.client.handlers.WaterSplashHandler;

public class FabricWaterSplashHandler extends WaterSplashHandler {

    @Override
    protected void addCustomSplashCloud(Minecraft mc, Vec3 pos) {
        for (int i = 0; i < 4; i++) {
            mc.level.addParticle(ExampleMod.SPLASH_CLOUD,
                pos.x + (Math.random() - 0.5) * 0.5,
                pos.y + 0.3,
                pos.z + (Math.random() - 0.5) * 0.5,
                (Math.random() - 0.5) * 0.2,
                0.15,
                (Math.random() - 0.5) * 0.2);
        }
    }
}
