package dev.ninix.visor.watersplash.fabric;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import dev.ninix.visor.watersplash.core.client.ExampleAddonClient;
import dev.ninix.visor.watersplash.core.server.ExampleAddonServer;
import net.fabricmc.api.ModInitializer;

public class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        if(ModLoader.get().isDedicatedServer()){
            VisorAPI.registerAddon(
                    new ExampleAddonServer()
            );
        }else{
            VisorAPI.registerAddon(
                new ExampleAddonClient()
            );

            new WaterSplashHandler();
        }
    }
}
