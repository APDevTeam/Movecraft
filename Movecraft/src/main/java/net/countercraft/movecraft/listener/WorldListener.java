package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.Set;

public class WorldListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        if (world == null) {
            // Weird, but ok...
            return;
        }
        Set<Craft> craftsInWorld = CraftManager.getInstance().getCraftsInWorld(world);
        if (craftsInWorld == null || craftsInWorld.isEmpty()) {
            return;
        }
        boolean allSuccessful = true;
        for (Craft craft : craftsInWorld) {
            // Clear the collapsed hitbox, otherwise a wreck task will be queued!
            craft.getCollapsedHitBox().clear();
            if (!CraftManager.getInstance().release(craft, CraftReleaseEvent.Reason.FORCE, true)) {
                allSuccessful = false;
            }
        }
        if (!allSuccessful) {
            event.setCancelled(true);
            // If unsuccessful, try to unload the world later
            Bukkit.getScheduler().runTaskLater(Movecraft.getInstance(), () -> {
                Bukkit.unloadWorld(world, true);
            }, 10);
        }
    }

}
