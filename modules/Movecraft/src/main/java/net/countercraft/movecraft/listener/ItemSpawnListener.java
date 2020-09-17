package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

public class ItemSpawnListener implements Listener {
    // prevent items from dropping from moving crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemSpawn(final ItemSpawnEvent e) {
        if (e.isCancelled()) {
            return;
        }
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(e.getLocation().getWorld())) {
            if ((!tcraft.isNotProcessing()) && MathUtils.locationInHitBox(tcraft.getHitBox(), e.getLocation())) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
