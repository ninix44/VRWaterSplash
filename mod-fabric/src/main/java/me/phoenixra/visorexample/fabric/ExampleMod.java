package me.phoenixra.visorexample.fabric;

import me.phoenixra.visor.api.ModLoader;
import me.phoenixra.visor.api.VisorAPI;
import me.phoenixra.visorexample.core.client.ExampleAddonClient;
import me.phoenixra.visorexample.core.server.ExampleAddonServer;
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
        }
    }
}
