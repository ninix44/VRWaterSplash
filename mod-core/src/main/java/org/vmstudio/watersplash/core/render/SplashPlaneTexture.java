package org.vmstudio.watersplash.core.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

public class SplashPlaneTexture {
    public final DynamicTexture texture;
    public final int resolution;

    public SplashPlaneTexture(int resolution) {
        this.resolution = resolution;
        this.texture = new DynamicTexture(resolution, resolution, false);
    }

    public void loadTexture(NativeImage image) {
        if (image == null) {
            return;
        }
        NativeImage textureImage = this.texture.getPixels();
        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {
                int color = image.getPixelRGBA(x, y);
                textureImage.setPixelRGBA(x, y, color);
            }
        }
        this.texture.upload();
    }

    public DynamicTexture getTexture() {
        return this.texture;
    }
}
