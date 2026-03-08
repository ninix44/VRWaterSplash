package org.vmstudio.watersplash.forge;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.watersplash.core.client.handlers.TickHandlerRegistry;
import org.vmstudio.watersplash.core.common.VisorExample;
import org.vmstudio.watersplash.core.server.ExampleAddonServer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mod(VisorExample.MOD_ID)
public class ExampleMod {
    private static final List<Consumer<Minecraft>> tickHandlers = new ArrayList<>();

    public ExampleMod(){
        TickHandlerRegistry.registerHandler = tickHandlers::add;

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientTickHandler::register);

        if(ModLoader.get().isDedicatedServer()){
            VisorAPI.registerAddon(
                    new ExampleAddonServer()
            );
        }else{
            VisorAPI.registerAddon(
                    new ForgeAddonClient()
            );
        }
    }

    @Mod.EventBusSubscriber(modid = VisorExample.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    private static class ClientTickHandler {
        public static void register() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                Minecraft mc = Minecraft.getInstance();
                tickHandlers.forEach(handler -> handler.accept(mc));
            }
        }
    }
}
