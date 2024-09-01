package net.countercraft.movecraft.sign;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public final class PilotSign implements Listener {
    private static final String HEADER = "Pilot:";
    @EventHandler
    public final void onSignChange(SignChangeEvent event){
        if (event.getLine(0).equalsIgnoreCase(HEADER)) {
            String pilotName = ChatColor.stripColor(event.getLine(1));
            if (pilotName.isEmpty()) {
                event.setLine(1, event.getPlayer().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignClickEvent(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }

        Sign sign = (Sign) block.getState();
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER)) {
            return;
        }
        event.setCancelled(true);
    }
}
