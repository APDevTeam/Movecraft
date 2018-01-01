package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class MoveSign implements Listener{
    private static final String HEADER = "Move:";

    @EventHandler
    public final void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN) {
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
        int dx = Integer.parseInt(numbers[0]);
        int dy = Integer.parseInt(numbers[1]);
        int dz = Integer.parseInt(numbers[2]);
        int maxMove = CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().maxStaticMove();

        if (dx > maxMove)
            dx = maxMove;
        if (dx < 0 - maxMove)
            dx = 0 - maxMove;
        if (dy > maxMove)
            dy = maxMove;
        if (dy < 0 - maxMove)
            dy = 0 - maxMove;
        if (dz > maxMove)
            dz = maxMove;
        if (dz < 0 - maxMove)
            dz = 0 - maxMove;

        if (!event.getPlayer().hasPermission("movecraft." + CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCraftName() + ".move")) {
            event.getPlayer().sendMessage(
                    I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }
        if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanStaticMove()) {
            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).translate(dx, dy, dz);
            //timeMap.put(event.getPlayer(), System.currentTimeMillis());
            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setLastCruisUpdate(System.currentTimeMillis());
        }
    }
}
