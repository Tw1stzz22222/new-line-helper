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
    private static final String KEY_LAUNCH = "已获得启动名额";
    private static final String KEY_FREQUENT = "同一时刻地牢创建频繁";
    private static final String KEY_LV = "LV";

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String msg = event.getMessage().getUnformattedText();

        // 含 LV 的玩家信息 → HUD（聊天框保留，不抹去）
        if (msg.contains(KEY_LV)) {
            HudOverlay.addLv(event.getMessage().getFormattedText());
        }

        if (!ModState.running) return;

        if (msg.contains(KEY_KILL)) {
            DungeonAutomation.killCount++;
            LOG.info("[auto] kill detected (total {})", DungeonAutomation.killCount);
        }
        if (msg.contains(KEY_REWARD) && DungeonAutomation.isAtRewardPhase()) {
            DungeonAutomation.rewardSeen = true;
            LOG.info("[auto] reward detected");
        }
        if (msg.contains(KEY_BUSY)) {
            DungeonAutomation.busySeen = true;
            LOG.info("[auto] busy detected");
        }
        if (msg.contains(KEY_LAUNCH)) {
            DungeonAutomation.launchSeen = true;
            LOG.info("[auto] launch detected");
        }
        if (msg.contains(KEY_FREQUENT)) {
            DungeonAutomation.frequentSeen = true;
            LOG.info("[auto] frequent create detected");
        }

        ClickEvent leave = findClickEvent(event.getMessage(), KEY_LEAVE);
        if (leave != null && leave.getAction() == ClickEvent.Action.RUN_COMMAND
                && DungeonAutomation.isAtRewardPhase()) {
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
