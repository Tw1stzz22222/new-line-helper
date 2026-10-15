package com.new_line_transcript;

import net.minecraft.block.BlockSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

@SideOnly(Side.CLIENT)
public class DungeonAutomation {

    private static final Logger LOG = LogManager.getLogger("new_line_transcript");

    // 15c dungeon waypoints (from 15c副本.txt)
    private static final int A_X = -45, A_Y = 14, A_Z = 10;
    private static final int B_X = -4, B_Y = 14, B_Z = 11;
    private static final int C_X = -5, C_Y = 14, C_Z = -8;

    private static final double ARRIVE_DIST = 2.0;          // arrival threshold for a goto
    private static final long WAIT_AFTER_ENTER = 2000L;   // 2s dungeon load
    private static final long WAIT_AT_A = 1000L;          // 1s at waypoint A
    private static final long WAIT_AT_B = 8000L;          // 8s at waypoint B
    private static final long REWARD_TIMEOUT = 60000L;    // safety net for reward
    private static final long LEAVE_WAIT = 10000L;        // 10s after leave click before closing flight
    private static final long LEAVE_TIMEOUT = 8000L;      // safety net for leave
    private static final long CLICK_DELAY = 600L;         // 0.6s between menu clicks
    private static final long BUSY_WAIT = 10000L;        // 10s wait when dungeon is busy

    private static final String BTN_CHALLENGE = "独立副本挑战";
    private static final String BTN_FLOOR_15 = "第十五层";
    private static final String BTN_ANNIHILATE = "湮灭";
    private static final String BTN_ENTER = "进入副本";

    private enum State {
        FIND_SIGN, SIGN_MENU, WAIT_BUSY, CHALLENGE_FLOOR, CHALLENGE_ANNIHILATE, CHALLENGE_ENTER,
        ENTERING, FLY_TO_A, FLY_TO_B, FLY_TO_C, LEAVE, FLY_OFF
    }

    private static State state = State.FIND_SIGN;
    private static long stateStartTime = 0L;
    private static long lastClickTime = 0L;
    private static long leaveSentTime = 0L;
    private static long arrivedTime = 0L;

    // Chat flags, updated by ChatListener.
    public static int killCount = 0;
    public static boolean rewardSeen = false;
    public static boolean leaveSent = false;
    public static boolean busySeen = false;

    public static void reset() {
        state = State.FIND_SIGN;
        stateStartTime = 0L;
        lastClickTime = 0L;
        leaveSentTime = 0L;
        arrivedTime = 0L;
        killCount = 0;
        rewardSeen = false;
        leaveSent = false;
        busySeen = false;
        LOG.info("[auto] reset to FIND_SIGN");
    }

    private static void setState(State s) {
        if (s != state) {
            LOG.info("[auto] state {} -> {}", state, s);
        }
        state = s;
    }

    // True only while we are at/heading to the reward waypoint C (or leaving).
    // Chat reward/leave signals are only honored in this phase, so late messages
    // from a previous run can't leak into the next run's A/B flight.
    public static boolean isAtRewardPhase() {
        return state == State.FLY_TO_C || state == State.LEAVE;
    }

    // True while flying between waypoints. Opening any GUI pauses player movement,
    // which would stall the flight, so we auto-close unexpected screens here.
    private static boolean isFlyingState() {
        return state == State.FLY_TO_A || state == State.FLY_TO_B || state == State.FLY_TO_C;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.world == null) return;
        long now = System.currentTimeMillis();

        // 飞行阶段自动关闭意外打开的界面（背包E/设置ESC/聊天T），否则移动暂停会打断飞行。
        if (isFlyingState() && mc.currentScreen != null) {
            LOG.info("[auto] closing unexpected GUI {} during flight", mc.currentScreen.getClass().getSimpleName());
            mc.displayGuiScreen(null);
        }

        switch (state) {
            case FIND_SIGN: {
                if (mc.currentScreen != null) {
                    setState(State.SIGN_MENU);
                    break;
                }
                BlockPos sign = findNearestSign(mc);
                if (sign != null) {
                    LOG.info("[auto] right-clicking sign at {}", sign);
                    rightClickBlock(mc, sign);
                    setState(State.SIGN_MENU);
                }
                break;
            }
            case SIGN_MENU: {
                if (busySeen) {
                    busySeen = false;
                    LOG.info("[auto] dungeon busy, retrying in {}s", BUSY_WAIT / 1000);
                    setState(State.WAIT_BUSY);
                    stateStartTime = now;
                    break;
                }
                if (mc.currentScreen != null && clickByText(mc.currentScreen, BTN_CHALLENGE)) {
                    setState(State.CHALLENGE_FLOOR);
                }
                break;
            }
            case WAIT_BUSY: {
                if (now - stateStartTime >= BUSY_WAIT) {
                    setState(State.FIND_SIGN);
                }
                break;
            }
            case CHALLENGE_FLOOR: {
                if (mc.currentScreen != null && clickByText(mc.currentScreen, BTN_FLOOR_15)) {
                    setState(State.CHALLENGE_ANNIHILATE);
                }
                break;
            }
            case CHALLENGE_ANNIHILATE: {
                if (mc.currentScreen != null && clickByText(mc.currentScreen, BTN_ANNIHILATE)) {
                    setState(State.CHALLENGE_ENTER);
                }
                break;
            }
            case CHALLENGE_ENTER: {
                if (mc.currentScreen != null && clickByText(mc.currentScreen, BTN_ENTER)) {
                    setState(State.ENTERING);
                    stateStartTime = now;
                }
                break;
            }
            case ENTERING: {
                if (now - stateStartTime >= WAIT_AFTER_ENTER) {
                    // Fresh run: discard any reward/leave flags left over from the previous run.
                    rewardSeen = false;
                    leaveSent = false;
                    killCount = 0;
                    setState(State.FLY_TO_A);
                    stateStartTime = now;
                }
                break;
            }
            case FLY_TO_A: {
                if (near(A_X, A_Y, A_Z)) {
                    releaseFlightKeys(mc);
                    if (arrivedTime == 0L) {
                        arrivedTime = now;
                    }
                    if (now - arrivedTime >= WAIT_AT_A) {
                        arrivedTime = 0L;
                        setState(State.FLY_TO_B);
                        stateStartTime = now;
                    }
                } else {
                    flyToward(mc, A_X, A_Y, A_Z);
                }
                break;
            }
            case FLY_TO_B: {
                if (near(B_X, B_Y, B_Z)) {
                    releaseFlightKeys(mc);
                    if (arrivedTime == 0L) {
                        arrivedTime = now;
                    }
                    if (now - arrivedTime >= WAIT_AT_B) {
                        arrivedTime = 0L;
                        setState(State.FLY_TO_C);
                        stateStartTime = now;
                    }
                } else {
                    flyToward(mc, B_X, B_Y, B_Z);
                }
                break;
            }
            case FLY_TO_C: {
                if (rewardSeen || leaveSent || now - stateStartTime >= REWARD_TIMEOUT) {
                    LOG.info("[auto] FLY_TO_C early exit: rewardSeen={} leaveSent={} timeout={}",
                            rewardSeen, leaveSent, now - stateStartTime >= REWARD_TIMEOUT);
                    releaseFlightKeys(mc);
                    setState(State.LEAVE);
                    stateStartTime = now;
                } else if (!near(C_X, C_Y, C_Z)) {
                    flyToward(mc, C_X, C_Y, C_Z);
                } else {
                    releaseFlightKeys(mc);
                }
                break;
            }
            case LEAVE: {
                if (leaveSent && leaveSentTime == 0L) {
                    leaveSentTime = now;
                }
                boolean waited = leaveSent && (now - leaveSentTime >= LEAVE_WAIT);
                if (waited || (!leaveSent && now - stateStartTime >= LEAVE_TIMEOUT)) {
                    setState(State.FLY_OFF);
                }
                break;
            }
            case FLY_OFF: {
                releaseFlightKeys(mc);
                reset();
                break;
            }
        }
    }

    private static boolean near(int x, int y, int z) {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.player != null && mc.player.getDistance(x + 0.5, y + 0.5, z + 0.5) < ARRIVE_DIST;
    }

    private static void flyToward(Minecraft mc, int tx, int ty, int tz) {
        net.minecraft.client.entity.EntityPlayerSP p = mc.player;
        boolean flying = p.capabilities != null && p.capabilities.isFlying;
        if (!flyingKnown || flying != lastFlying) {
            flyingKnown = true;
            lastFlying = flying;
            LOG.warn("[auto] flight state = {}", flying);
            if (!flying) {
                p.sendMessage(new TextComponentString("§b§l[新一线]§r §e[飞行]§c 检测到未在飞行，自动化飞行可能无效"));
            }
        }
        double dx = (tx + 0.5) - p.posX;
        double dy = (ty + 0.5) - p.posY;
        double dz = (tz + 0.5) - p.posZ;

        p.rotationYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;

        setKey(mc.gameSettings.keyBindForward, true);
        if (dy > 1.0) {
            setKey(mc.gameSettings.keyBindJump, true);
            setKey(mc.gameSettings.keyBindSneak, false);
        } else if (dy < -1.0) {
            setKey(mc.gameSettings.keyBindJump, false);
            setKey(mc.gameSettings.keyBindSneak, true);
        } else {
            setKey(mc.gameSettings.keyBindJump, false);
            setKey(mc.gameSettings.keyBindSneak, false);
        }
    }

    public static void releaseFlightKeys(Minecraft mc) {
        setKey(mc.gameSettings.keyBindForward, false);
        setKey(mc.gameSettings.keyBindJump, false);
        setKey(mc.gameSettings.keyBindSneak, false);
    }

    private static void setKey(KeyBinding kb, boolean pressed) {
        if (kb != null) {
            KeyBinding.setKeyBindState(kb.getKeyCode(), pressed);
        }
    }

    private static BlockPos findNearestSign(Minecraft mc) {
        EntityPlayer p = mc.player;
        BlockPos center = p.getPosition();
        int r = 6;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.getAllInBox(center.add(-r, -3, -r), center.add(r, 3, r))) {
            IBlockState st = mc.world.getBlockState(pos);
            if (st.getBlock() instanceof BlockSign) {
                double d = p.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (d < bestDist) {
                    bestDist = d;
                    best = pos;
                }
            }
        }
        if (best == null) {
            LOG.warn("[auto] no sign found within radius {}", r);
        }
        return best;
    }

    private static void rightClickBlock(Minecraft mc, BlockPos pos) {
        net.minecraft.client.entity.EntityPlayerSP p = mc.player;
        Vec3d eye = p.getPositionEyes(1.0F);
        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3d dir = center.subtract(eye).normalize();
        EnumFacing side = EnumFacing.getFacingFromVector((float) dir.x, (float) dir.y, (float) dir.z);
        mc.playerController.processRightClickBlock(p, mc.world, pos, side, center, EnumHand.MAIN_HAND);
        p.swingArm(EnumHand.MAIN_HAND);
    }

    private static boolean clickByText(GuiScreen screen, String text) {
        if (System.currentTimeMillis() - lastClickTime < CLICK_DELAY) return false;
        List<GuiButton> buttons = getButtons(screen);
        for (GuiButton b : buttons) {
            if (b.visible && b.enabled && b.displayString != null && b.displayString.contains(text)) {
                LOG.info("[auto] clicking button '{}' on {} (id={})", text, screen.getClass().getSimpleName(), b.id);
                b.playPressSound(Minecraft.getMinecraft().getSoundHandler());
                invokeActionPerformed(screen, b);
                lastClickTime = System.currentTimeMillis();
                return true;
            }
        }
        if (clickSlotByText(screen, text)) {
            lastClickTime = System.currentTimeMillis();
            return true;
        }
        dumpMenu(screen, text, buttons);
        return false;
    }

    private static boolean clickSlotByText(GuiScreen screen, String text) {
        if (!(screen instanceof GuiContainer)) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return false;
        Container c = mc.player.openContainer;
        for (Slot slot : c.inventorySlots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            String name = stack.getDisplayName();
            if (name != null && name.contains(text)) {
                LOG.info("[auto] clicking slot {} '{}' for '{}' on {}", slot.slotNumber, name, text, screen.getClass().getSimpleName());
                mc.playerController.windowClick(c.windowId, slot.slotNumber, 0, ClickType.PICKUP, mc.player);
                return true;
            }
        }
        return false;
    }

    private static String lastDumpTarget = null;

    private static boolean lastFlying = false;
    private static boolean flyingKnown = false;

    private static void dumpMenu(GuiScreen screen, String target, List<GuiButton> buttons) {
        if (target.equals(lastDumpTarget)) return;
        lastDumpTarget = target;
        LOG.warn("[auto] no '{}' on {} ({} buttons)", target, screen.getClass().getSimpleName(), buttons.size());
        for (GuiButton b : buttons) {
            LOG.warn("[auto]   btn[{}] {} , {} {}x{} '{}' visible={} enabled={}",
                    b.id, b.x, b.y, b.width, b.height, b.displayString, b.visible, b.enabled);
        }
        if (screen instanceof GuiContainer) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player != null) {
                Container c = mc.player.openContainer;
                for (Slot slot : c.inventorySlots) {
                    ItemStack stack = slot.getStack();
                    if (!stack.isEmpty()) {
                        LOG.warn("[auto]   slot {} '{}'", slot.slotNumber, stack.getDisplayName());
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<GuiButton> getButtons(GuiScreen screen) {
        try {
            Field f = GuiScreen.class.getDeclaredField("field_146292_n"); // SRG name for buttonList
            f.setAccessible(true);
            return (List<GuiButton>) f.get(screen);
        } catch (Exception e) {
            LOG.warn("[auto] SRG field lookup failed: {}", e.toString());
        }
        try {
            Field f = GuiScreen.class.getDeclaredField("buttonList"); // MCP name (dev only)
            f.setAccessible(true);
            return (List<GuiButton>) f.get(screen);
        } catch (Exception e) {
            LOG.warn("[auto] MCP field lookup failed: {}", e.toString());
        }
        return Collections.emptyList();
    }

    private static void invokeActionPerformed(GuiScreen screen, GuiButton button) {
        try {
            Method m = GuiScreen.class.getDeclaredMethod("func_146284_a", GuiButton.class); // SRG
            m.setAccessible(true);
            m.invoke(screen, button);
            return;
        } catch (Exception e) {
            LOG.warn("[auto] SRG actionPerformed failed: {}", e.toString());
        }
        try {
            Method m = GuiScreen.class.getDeclaredMethod("actionPerformed", GuiButton.class); // MCP (dev only)
            m.setAccessible(true);
            m.invoke(screen, button);
        } catch (Exception e) {
            LOG.warn("[auto] MCP actionPerformed failed: {}", e.toString());
        }
    }

    // Diagnostic: dump the current screen's buttons and mouse coordinates.
    public static void dumpScreen(Minecraft mc) {
        GuiScreen s = mc.currentScreen;
        if (s == null) {
            LOG.info("[auto] dump: no screen open");
            if (mc.player != null) mc.player.sendMessage(new TextComponentString("§b§l[新一线]§r §e[调试]§7 没有打开的界面"));
            return;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        int mx = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int my = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;
        LOG.info("[auto] dump: screen={} mouse={},{}", s.getClass().getSimpleName(), mx, my);
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponentString("§b§l[新一线]§r §e[调试]§7 界面=" + s.getClass().getSimpleName() + " 鼠标=" + mx + "," + my));
        }
        for (GuiButton b : getButtons(s)) {
            LOG.info("[auto] dump: btn[{}] {} , {} {}x{} '{}'", b.id, b.x, b.y, b.width, b.height, b.displayString);
            if (mc.player != null) {
                mc.player.sendMessage(new TextComponentString("§b§l[新一线]§r §e[调试]§7 按钮[" + b.id + "] " + b.x + "," + b.y + " " + b.width + "x" + b.height + " '" + b.displayString + "'"));
            }
        }
        if (s instanceof GuiContainer && mc.player != null) {
            Container c = mc.player.openContainer;
            for (Slot slot : c.inventorySlots) {
                ItemStack stack = slot.getStack();
                if (stack.isEmpty()) continue;
                LOG.info("[auto] dump: slot {} '{}'", slot.slotNumber, stack.getDisplayName());
                if (mc.player != null) {
                    mc.player.sendMessage(new TextComponentString("§b§l[新一线]§r §e[调试]§7 格子 " + slot.slotNumber + " '" + stack.getDisplayName() + "'"));
                }
            }
        }
    }
}
