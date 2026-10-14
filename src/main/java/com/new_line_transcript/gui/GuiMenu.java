package com.new_line_transcript.gui;

import com.new_line_transcript.ClientEventHandler;
import com.new_line_transcript.DungeonAutomation;
import com.new_line_transcript.ModState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

@SideOnly(Side.CLIENT)
public class GuiMenu extends GuiScreen {

    private static final int ID_ENABLE = 0;
    private static final int PANEL_W = 240;
    private static final int PANEL_H = 132;
    private static final char[] GRADIENT = {'3', 'b', 'f'};

    private int panelX;
    private int panelY;

    @Override
    public void initGui() {
        this.buttonList.clear();
        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;

        int btnW = 150;
        int btnH = 22;
        this.buttonList.add(new FlatButton(ID_ENABLE,
                (this.width - btnW) / 2, panelY + 64, btnW, btnH, "Enable"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == ID_ENABLE) {
            DungeonAutomation.reset();
            ModState.start();
            if (this.mc.player != null) {
                this.mc.player.sendMessage(new TextComponentString("§b§l[新一线]§r §a自动化已开启"));
            }
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_EQUALS) {
            ClientEventHandler.terminate(this.mc);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawPanel();
        this.drawHeader();
        this.drawFooter();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPanel() {
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xCC101418);
        int border = 0xFF2BB0A8;
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + 1, border);
        drawRect(panelX, panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, border);
        drawRect(panelX, panelY, panelX + 1, panelY + PANEL_H, border);
        drawRect(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + PANEL_H, border);
    }

    private void drawHeader() {
        this.drawCenteredString(this.fontRenderer, gradient("Dungeon Automation"), this.width / 2, panelY + 14, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, "§7§oFloor 15c", this.width / 2, panelY + 34, 0xFFFFFF);
    }

    private void drawFooter() {
        this.drawCenteredString(this.fontRenderer, "§8= Terminate    ·    ` Debug", this.width / 2, panelY + PANEL_H - 14, 0xFFFFFF);
    }

    private static String gradient(String text) {
        StringBuilder sb = new StringBuilder("§l");
        int n = text.length();
        for (int i = 0; i < n; i++) {
            int idx = (int) Math.round((double) i / Math.max(1, n - 1) * (GRADIENT.length - 1));
            sb.append('§').append(GRADIENT[idx]).append(text.charAt(i));
        }
        return sb.toString();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static class FlatButton extends GuiButton {
        FlatButton(int id, int x, int y, int w, int h, String text) {
            super(id, x, y, w, h, text);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) return;
            FontRenderer fr = mc.fontRenderer;
            boolean hovered = mouseX >= this.x && mouseY >= this.y
                    && mouseX < this.x + this.width && mouseY < this.y + this.height;

            drawRect(this.x, this.y, this.x + this.width, this.y + this.height,
                    hovered ? 0xF026A65B : 0xE01E8E4F);
            int edge = 0xFF55D98C;
            drawRect(this.x, this.y, this.x + this.width, this.y + 1, edge);
            drawRect(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, edge);
            drawRect(this.x, this.y, this.x + 1, this.y + this.height, edge);
            drawRect(this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, edge);

            this.drawCenteredString(fr, this.displayString,
                    this.x + this.width / 2, this.y + (this.height - 8) / 2,
                    hovered ? 0xFFFFFF : 0xDFFFDF);
        }
    }
}
