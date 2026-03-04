package org.vmstudio.watersplash.fabric;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.watersplash.core.client.ExampleAddonClient;
import org.vmstudio.watersplash.core.server.ExampleAddonServer;
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
