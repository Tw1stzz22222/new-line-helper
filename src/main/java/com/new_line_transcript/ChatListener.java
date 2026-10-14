package com.new_line_transcript;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SideOnly(Side.CLIENT)
public class ChatListener {

    private static final Logger LOG = LogManager.getLogger("new_line_transcript");

    private static final String KEY_KILL = "成功击杀";
    private static final String KEY_REWARD = "通关奖励已放入背包";
    private static final String KEY_LEAVE = "点击立即离开副本";
    private static final String KEY_BUSY = "同一时间";

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!ModState.running) return;
        String msg = event.getMessage().getUnformattedText();

        if (msg.contains(KEY_KILL)) {
            DungeonAutomation.killCount++;
            LOG.info("[auto] kill detected (total {})", DungeonAutomation.killCount);
        }
        if (msg.contains(KEY_REWARD)) {
            DungeonAutomation.rewardSeen = true;
            LOG.info("[auto] reward detected");
        }
        if (msg.contains(KEY_BUSY)) {
            DungeonAutomation.busySeen = true;
            LOG.info("[auto] busy detected");
        }

        ClickEvent leave = findClickEvent(event.getMessage(), KEY_LEAVE);
        if (leave != null && leave.getAction() == ClickEvent.Action.RUN_COMMAND) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player != null) {
                LOG.info("[auto] clicking leave link: {}", leave.getValue());
                mc.player.sendChatMessage(leave.getValue());
                DungeonAutomation.leaveSent = true;
            }
        }
    }

    private ClickEvent findClickEvent(ITextComponent comp, String text) {
        ClickEvent own = comp.getStyle().getClickEvent();
        if (own != null && comp.getUnformattedText().contains(text)) {
            return own;
        }
        for (ITextComponent sib : comp.getSiblings()) {
            ClickEvent found = findClickEvent(sib, text);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
