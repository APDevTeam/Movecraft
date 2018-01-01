package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class AscendSign implements Listener {


    @EventHandler
    public void onSignClickEvent(PlayerInteractEvent event){
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN){
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: OFF")) {
            if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) == null) {
                return;
            }
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (!c.getType().getCanCruise()) {
                return;
            }
            c.resetSigns(true, false, true);
            sign.setLine(0, "Ascend: ON");
            sign.update(true);

            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruiseDirection((byte) 0x42);
            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setLastCruisUpdate(System.currentTimeMillis());
            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruising(true);

            if (!c.getType().getMoveEntities()) {
                CraftManager.getInstance().addReleaseTask(c);
            }
            return;
        }
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: ON")) {
            return;
        }
        if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) == null || !CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanCruise()) {
            return;
        }
        sign.setLine(0, "Ascend: OFF");
        sign.update(true);
        CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruising(false);
    }
}
