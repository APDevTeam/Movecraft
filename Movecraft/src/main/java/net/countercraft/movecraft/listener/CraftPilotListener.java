package net.countercraft.movecraft.listener;

import jakarta.inject.Inject;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftPilotEvent;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class CraftPilotListener implements Listener {
    @Inject
    public CraftPilotListener(){}

    @EventHandler(ignoreCancelled = true)
    public void onCraftPilot(@NotNull CraftPilotEvent event) {
        // Walk through all signs and set a UUID in there
        final Craft craft = event.getCraft();

        // Now, find all signs on the craft...
        for (MovecraftLocation mLoc : craft.getHitBox()) {
            Block block = mLoc.toBukkit(craft.getWorld()).getBlock();
            // Only interested in signs, if no sign => continue
            // TODO: Just limit to signs?
            // Edit: That's useful for dispensers too to flag TNT and the like, but for that one could use a separate listener
            if (!(block.getState() instanceof Sign))
                continue;
            // Sign located!
            Sign tile = (Sign) block.getState();

            craft.markTileStateWithUUID(tile);
            tile.update();
        }
    }

}
