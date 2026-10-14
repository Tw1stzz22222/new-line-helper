package com.new_line_transcript;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class KeyBindings {
    public static final KeyBinding OPEN_MENU = new KeyBinding("key.new_line_transcript.menu", Keyboard.KEY_M, "key.categories.new_line_transcript");
    public static final KeyBinding TERMINATE = new KeyBinding("key.new_line_transcript.terminate", Keyboard.KEY_EQUALS, "key.categories.new_line_transcript");
    public static final KeyBinding DEBUG = new KeyBinding("key.new_line_transcript.debug", Keyboard.KEY_GRAVE, "key.categories.new_line_transcript");
}
