package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.craft.CraftManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    @EventHandler
    public void onPLayerLogout(PlayerQuitEvent e) {
        CraftManager.getInstance().removeCraftByPlayer(e.getPlayer());
    }
}
