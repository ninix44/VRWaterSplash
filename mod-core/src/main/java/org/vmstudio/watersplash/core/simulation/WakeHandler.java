package org.vmstudio.watersplash.core.simulation;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.vmstudio.watersplash.core.config.WakesConfig;
import org.vmstudio.watersplash.core.render.WakeTextureAtlas;
import org.vmstudio.watersplash.core.utils.QueueSet;

import java.util.*;

public class WakeHandler {
    private static WakeHandler INSTANCE;
    public Level world;

    private final HashMap<WakeChunkPos, WakeChunk> wakeChunks = new HashMap<>();
    private final QueueSet<WakeNode> toBeInserted;

    public static int resolution = WakesConfig.wakeResolution.res;
    public static int power = WakesConfig.wakeResolution.power;
    private WakeTextureAtlas textureAtlas;

    private WakeHandler(Level world) {
        this.world = world;
        this.toBeInserted = new QueueSet<>();
    }

    public static Optional<WakeHandler> getInstance() {
        if (INSTANCE == null) {
            if (Minecraft.getInstance().level == null) {
                return Optional.empty();
            }
            INSTANCE = new WakeHandler(Minecraft.getInstance().level);
        }
        return Optional.of(INSTANCE);
    }

    public static void init(Level world) {
        INSTANCE = new WakeHandler(world);
    }

    public static void kill() {
        if (INSTANCE != null) {
            INSTANCE.wakeChunks.clear();
        }
        INSTANCE = null;
    }

    public void tick() {
        if (WakesConfig.wakeResolution.res != WakeHandler.resolution) {
            WakeHandler.resolution = WakesConfig.wakeResolution.res;
            WakeHandler.power = WakesConfig.wakeResolution.power;
            if (textureAtlas != null) {
                textureAtlas.setResolution(resolution);
            }
            reset();
        } else {
            wakeLogic();
        }
    }

    private void wakeLogic() {
        ArrayList<WakeChunkPos> toBeRemovedChunks = new ArrayList<>();
        for (WakeChunk chunk : wakeChunks.values()) {
            boolean wakesPresent = chunk.tick();
            if (!wakesPresent) {
                chunk.destroy();
                toBeRemovedChunks.add(chunk.chunkPos);
            }
        }
        for (WakeChunkPos pos : toBeRemovedChunks) {
            wakeChunks.remove(pos);
        }

        while (toBeInserted.peek() != null) {
            WakeNode node = toBeInserted.poll();
            WakeChunkPos pos = WakeChunkPos.fromWakeNode(node);
            WakeChunk chunk = wakeChunks.get(pos);
            if (chunk == null) {
                chunk = new WakeChunk(pos, this);
                wakeChunks.put(pos, chunk);
            }
            chunk.insert(node);
        }
    }

    public WakeChunk getChunk(WakeChunkPos pos) {
        return wakeChunks.get(pos);
    }

    public void recolorWakes() {
        for (WakeChunk chunk : wakeChunks.values()) {
            chunk.drawWakes();
        }
    }

    public void insert(WakeNode node) {
        if (world == null) {
            return;
        }
        if (node.validPos(world)) {
            this.toBeInserted.add(node);
        }
    }

    public List<WakeNode> getVisibleNodes() {
        ArrayList<WakeNode> nodes = new ArrayList<>();
        for (WakeChunk chunk : wakeChunks.values()) {
            if (isInFrustum(chunk.boundingBox)) {
                chunk.query(nodes);
            }
        }
        return nodes;
    }

    public List<WakeChunk> getVisibleChunks() {
        ArrayList<WakeChunk> chunks = new ArrayList<>();
        for (WakeChunk chunk : wakeChunks.values()) {
            if (isInFrustum(chunk.boundingBox)) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    public boolean isInFrustum(AABB box) {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera.getPosition().distanceToSqr(box.getCenter()) > 64 * 64) {
            return false;
        }
        return true;
    }

    public WakeTextureAtlas getTextureAtlas() {
        if (textureAtlas == null) {
            textureAtlas = new WakeTextureAtlas();
            textureAtlas.setResolution(resolution);
        }
        return textureAtlas;
    }

    private void reset() {
        for (WakeChunk chunk : wakeChunks.values()) {
            chunk.destroy();
        }
        wakeChunks.clear();
        toBeInserted.clear();
    }
}
