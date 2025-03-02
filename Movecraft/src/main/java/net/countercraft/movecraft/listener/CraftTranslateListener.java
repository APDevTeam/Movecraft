package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class CraftTranslateListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCraftTranslated(final CraftTranslateEvent event) {
        Craft craft = event.getCraft();
        if (craft == null) {
            return;
        }
        final World world = craft.getWorld();
        for (MovecraftLocation movecraftLocation : craft.getHitBox().asSet()) {
            Block block = movecraftLocation.toBukkit(world).getBlock();
            BlockState state = block.getState();
            if (state instanceof Banner banner) {
                if (banner.getPatterns().size() > 0) {
                    banner.update(false, false);
                }
            }
        }
    }
}
