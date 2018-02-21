package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class DescendSign implements Listener{
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
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Descend: OFF")) {
            if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) == null) {
                return;
            }
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (!c.getType().getCanCruise()) {
                return;
            }
            //c.resetSigns(true, true, false);
            sign.setLine(0, "Descend: ON");
            sign.update(true);

            c.setCruiseDirection((byte) 0x43);
            c.setLastCruisUpdate(System.currentTimeMillis());
            c.setCruising(true);

            if (!c.getType().getMoveEntities()) {
                CraftManager.getInstance().addReleaseTask(c);
            }
            return;
        }
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Descend: ON")) {
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (c != null && c.getType().getCanCruise()) {
                sign.setLine(0, "Descend: OFF");
                sign.update(true);
                c.setCruising(false);
            }
        }
    }
}
