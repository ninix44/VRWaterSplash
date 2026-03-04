package org.vmstudio.watersplash.core.client.handlers;

import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public class TickHandlerRegistry {
    public static Consumer<Consumer<Minecraft>> registerHandler = null;
}
