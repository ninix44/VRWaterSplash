package org.vmstudio.watersplash.core.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.vmstudio.watersplash.core.config.WakesConfig;
import org.vmstudio.watersplash.core.simulation.WakeChunk;
import org.vmstudio.watersplash.core.simulation.WakeHandler;

import java.util.List;

public class WakeRenderer {

    private static final ResourceLocation WAKE_TEXTURE = new ResourceLocation("watersplash", "textures/particle/wake.png");

    public void render(PoseStack matrices) {
        if (WakesConfig.disableMod) {
            return;
        }

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;

        List<WakeChunk> wakeChunks = wakeHandler.getVisibleChunks();
        if (wakeChunks.isEmpty()) return;

        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        matrices.pushPose();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Matrix4f matrix = matrices.last().pose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bb = tesselator.getBuilder();

        Minecraft.getInstance().getTextureManager().bindForSetup(WAKE_TEXTURE);

        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

        for (WakeChunk chunk : wakeChunks) {
            if (chunk.drawContext != null) {
                addVertices(matrix, bb, chunk);
            }
        }

        tesselator.end();

        matrices.popPose();
    }

    private void addVertices(Matrix4f matrix, BufferBuilder bb, WakeChunk chunk) {
        var uv = chunk.drawContext.getUV();
        float uvOffset = chunk.drawContext.getUVOffset();

        float x0 = (float) chunk.pos.x;
        float y = (float) (chunk.pos.y + 0.888f);
        float z0 = (float) chunk.pos.z;

        float x1 = x0 + WakeChunk.WIDTH;
        float z1 = z0 + WakeChunk.WIDTH;

        bb.vertex(matrix, x0, y, z0)
                .uv(uv.u(), uv.v())
                .color(1.0f, 1.0f, 1.0f, WakesConfig.wakeOpacity)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();
        bb.vertex(matrix, x0, y, z1)
                .uv(uv.u(), uv.v() + uvOffset)
                .color(1.0f, 1.0f, 1.0f, WakesConfig.wakeOpacity)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();
        bb.vertex(matrix, x1, y, z1)
                .uv(uv.u() + uvOffset, uv.v() + uvOffset)
                .color(1.0f, 1.0f, 1.0f, WakesConfig.wakeOpacity)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();
        bb.vertex(matrix, x1, y, z0)
                .uv(uv.u() + uvOffset, uv.v())
                .color(1.0f, 1.0f, 1.0f, WakesConfig.wakeOpacity)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();
    }
}
