package com.new_line_transcript;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class HudOverlay {

    private static final int MAX_LINES = 8;
    private static final int LINE_HEIGHT = 10;

    private static final int BOX_W = 600;      // 固定框宽
    private static final int PAD_X = 8;        // 框内左右留白
    private static final int PAD_Y = 4;        // 框内上下留白
    private static final int BOX_GAP = 6;      // 框之间的间距
    private static final int BOX_BG = 0xCC101418;
    private static final int BOX_BORDER = 0xFF2BB0A8;

    private static final int STATE_LINES = 1;          // 状态框固定 1 行
    private static final int LV_LINES = MAX_LINES;     // LV 框固定 8 行

    private static String stateText = "";
    private static final Deque<String> lvLines = new LinkedList<>();

    public static void setState(String text) {
        stateText = text;
    }

    public static void addLv(String text) {
        push(lvLines, text);
    }

    private static void push(Deque<String> q, String text) {
        q.addFirst(text);
        while (q.size() > MAX_LINES) {
            q.removeLast();
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int w = sr.getScaledWidth();
        int right = w - 8;
        int y = 8;

        // 两个框永久固定显示，不管有没有内容
        y = renderBox(mc, Collections.singletonList(stateText), right, y, STATE_LINES);
        y = renderBox(mc, new ArrayList<>(lvLines), right, y, LV_LINES);
    }

    // 画一个固定宽高（行数）的框（右对齐），渲染 lines，返回下一个框的起始 y
    private static int renderBox(Minecraft mc, List<String> lines, int right, int y, int fixedLines) {
        List<String> expanded = new ArrayList<>();
        for (String line : lines) {
            for (String part : line.split("\\n", -1)) {
                expanded.add(part);
            }
        }

        int boxW = Math.min(BOX_W, right - 8);
        int x1 = right - boxW;
        int lineCount = Math.max(fixedLines, expanded.size());
        int boxH = lineCount * LINE_HEIGHT + PAD_Y * 2;

        Gui.drawRect(x1, y, right, y + boxH, BOX_BG);
        Gui.drawRect(x1, y, right, y + 1, BOX_BORDER);
        Gui.drawRect(x1, y + boxH - 1, right, y + boxH, BOX_BORDER);
        Gui.drawRect(x1, y, x1 + 1, y + boxH, BOX_BORDER);
        Gui.drawRect(right - 1, y, right, y + boxH, BOX_BORDER);

        int ty = y + PAD_Y;
        int maxTextW = boxW - PAD_X * 2;
        for (String line : expanded) {
            String s = mc.fontRenderer.trimStringToWidth(line, maxTextW);
            mc.fontRenderer.drawStringWithShadow(s, x1 + PAD_X, ty, 0xFFFFFF);
            ty += LINE_HEIGHT;
        }
        return y + boxH + BOX_GAP;
    }
}
