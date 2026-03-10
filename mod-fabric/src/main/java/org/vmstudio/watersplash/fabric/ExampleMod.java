package org.vmstudio.watersplash.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.watersplash.core.client.ExampleAddonClient;
import org.vmstudio.watersplash.core.client.handlers.TickHandlerRegistry;
import org.vmstudio.watersplash.core.server.ExampleAddonServer;
import net.fabricmc.api.ModInitializer;

public class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        TickHandlerRegistry.registerHandler = (handler) -> {
            ClientTickEvents.END_CLIENT_TICK.register((mc) -> handler.accept(mc));
        };

        if(ModLoader.get().isDedicatedServer()){
            VisorAPI.registerAddon(
                    new ExampleAddonServer()
            );
        }else{
            VisorAPI.registerAddon(
                new ExampleAddonClient()
            );
        }
    }
}
