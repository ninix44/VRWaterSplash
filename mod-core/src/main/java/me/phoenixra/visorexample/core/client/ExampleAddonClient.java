package me.phoenixra.visorexample.core.client;

import me.phoenixra.visor.api.VisorAPI;
import me.phoenixra.visor.api.common.addon.VisorAddon;
import me.phoenixra.visorexample.core.client.overlays.VROverlayExample;
import me.phoenixra.visorexample.core.common.VisorExample;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExampleAddonClient implements VisorAddon {
    @Override
    public void onAddonLoad() {
        VisorAPI.addonManager().getRegistries()
                .overlays()
                .registerElements(
                        List.of(
                                new VROverlayExample(
                                        this,
                                        VROverlayExample.ID
                                )
                        )
                );
    }

    @Override
    public @Nullable String getAddonPackagePath() {
        return "me.phoenixra.visorexample.core.client";
    }

    @Override
    public @NotNull String getAddonId() {
        return VisorExample.MOD_ID;
    }
    @Override
    public @NotNull Component getAddonName() {
        return Component.literal(VisorExample.MOD_NAME);
    }
    @Override
    public String getModId() {
        return VisorExample.MOD_ID;
    }
}
