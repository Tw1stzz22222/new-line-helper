package com.new_line_transcript;

import com.new_line_transcript.gui.GuiMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (KeyBindings.OPEN_MENU.isPressed()) {
            if (mc.currentScreen == null && mc.player != null) {
                mc.displayGuiScreen(new GuiMenu());
            }
        }
        if (KeyBindings.TERMINATE.isPressed()) {
            terminate(mc);
        }
        if (KeyBindings.DEBUG.isPressed()) {
            DungeonAutomation.dumpScreen(mc);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ModState.running) {
            DungeonAutomation.tick(Minecraft.getMinecraft());
        }
    }

    public static void terminate(Minecraft mc) {
        ModState.terminate();
        DungeonAutomation.releaseFlightKeys(mc);
        if (mc.currentScreen != null) {
            mc.displayGuiScreen(null);
        }
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponentString("§b§l[新一线]§r §c自动化已终止"));
        }
    }
}
