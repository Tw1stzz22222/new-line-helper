package com.new_line_transcript;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        ClientRegistry.registerKeyBinding(KeyBindings.OPEN_MENU);
        ClientRegistry.registerKeyBinding(KeyBindings.TERMINATE);
        ClientRegistry.registerKeyBinding(KeyBindings.DEBUG);
    }

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new ChatListener());
    }
}
