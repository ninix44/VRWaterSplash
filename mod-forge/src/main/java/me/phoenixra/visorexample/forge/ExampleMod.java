package me.phoenixra.visorexample.forge;

import me.phoenixra.visor.api.ModLoader;
import me.phoenixra.visor.api.VisorAPI;
import me.phoenixra.visorexample.core.client.ExampleAddonClient;
import me.phoenixra.visorexample.core.common.VisorExample;
import me.phoenixra.visorexample.core.server.ExampleAddonServer;
import net.minecraftforge.fml.common.Mod;

@Mod(VisorExample.MOD_ID)
public class ExampleMod {
    public ExampleMod(){
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
