package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.SignUtils;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class RelativeMoveSign implements Listener{
    private static final String HEADER = "RMove:";

    @EventHandler
    public final void onSignClick(PlayerInteractEvent event) {
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
        int maxMove = CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().maxStaticMove();

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
        switch (SignUtils.getFacing(sign).getOppositeFace()) {
            case NORTH:
                // North
                dx = dLeftRight;
                dz = -dBackwardForward;
                break;
            case SOUTH:
                // South
                dx = -dLeftRight;
                dz = dBackwardForward;
                break;
            case EAST:
                // East
                dx = dBackwardForward;
                dz = dLeftRight;
                break;
            case WEST:
                // West
                dx = -dBackwardForward;
                dz = -dLeftRight;
                break;
        }

        if (!event.getPlayer().hasPermission("movecraft." + CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCraftName() + ".move")) {
            event.getPlayer().sendMessage(
                    I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }
        if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanStaticMove()) {
            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).translate(dx, dy, dz);
            //timeMap.put(event.getPlayer(), System.currentTimeMillis());
            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setLastCruiseUpdate(System.currentTimeMillis());
        }
    }
}
