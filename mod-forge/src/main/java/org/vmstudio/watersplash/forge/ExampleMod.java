package org.vmstudio.watersplash.forge;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.watersplash.core.client.ExampleAddonClient;
import org.vmstudio.watersplash.core.common.VisorExample;
import org.vmstudio.watersplash.core.server.ExampleAddonServer;
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
