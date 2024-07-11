package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public final class RelativeMoveSign implements Listener{
    private static final String HEADER = "RMove:";

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignClick(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER)) {
            return;
        }
        event.setCancelled(true);
        if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) == null) {
            return;
        }
        /*Long time = timeMap.get(event.getPlayer());
        if (time != null) {
            long ticksElapsed = (System.currentTimeMillis() - time) / 50;
            Craft craft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            // if the craft should go slower underwater, make time pass
            // more slowly there
            if (craft.getType().getHalfSpeedUnderwater() && craft.getMinY() < craft.getW().getSeaLevel()) {
                ticksElapsed = ticksElapsed >> 1;
            }

            if (Math.abs(ticksElapsed) < CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getTickCooldown()) {
                event.setCancelled(true);
                return;
            }
        }*/
        String[] numbers = ChatColor.stripColor(sign.getLine(1)).split(",");
        int dLeftRight = Integer.parseInt(numbers[0]); // negative =
        // left,
        // positive =
        // right
        int dy = Integer.parseInt(numbers[1]);
        int dBackwardForward = Integer.parseInt(numbers[2]); // negative
        // =
        // backwards,
        // positive
        // =
        // forwards
        int maxMove = CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getIntProperty(CraftType.MAX_STATIC_MOVE);

        if (dLeftRight > maxMove)
            dLeftRight = maxMove;
        if (dLeftRight < -maxMove)
            dLeftRight = -maxMove;
        if (dy > maxMove)
            dy = maxMove;
        if (dy < -maxMove)
            dy = -maxMove;
        if (dBackwardForward > maxMove)
            dBackwardForward = maxMove;
        if (dBackwardForward < -maxMove)
            dBackwardForward = -maxMove;
        int dx = 0;
        int dz = 0;
        switch (sign.getRawData()) {
            case 0x3:
                // North
                dx = dLeftRight;
                dz = -dBackwardForward;
                break;
            case 0x2:
                // South
                dx = -dLeftRight;
                dz = dBackwardForward;
                break;
            case 0x4:
                // East
                dx = dBackwardForward;
                dz = dLeftRight;
                break;
            case 0x5:
                // West
                dx = -dBackwardForward;
                dz = -dLeftRight;
                break;
        }

        if (!event.getPlayer().hasPermission("movecraft." + CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getStringProperty(CraftType.NAME) + ".move")) {
            event.getPlayer().sendMessage(
                    I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }
        if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getBoolProperty(CraftType.CAN_STATIC_MOVE)) {
            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).translate(dx, dy, dz);
            //timeMap.put(event.getPlayer(), System.currentTimeMillis());
            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setLastCruiseUpdate(System.currentTimeMillis());
        }
    }
}
