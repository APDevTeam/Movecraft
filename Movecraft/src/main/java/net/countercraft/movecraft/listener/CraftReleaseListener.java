package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class CraftReleaseListener implements Listener {

    @EventHandler
    public void onDisassembly(@NotNull CraftReleaseEvent event) {
        // Walk through all signs and set a UUID in there
        final Craft craft = event.getCraft();

        // Now, find all signs on the craft...
        for (MovecraftLocation mLoc : craft.getHitBox()) {
            Block block = mLoc.toBukkit(craft.getWorld()).getBlock();
            // Only interested in signs, if no sign => continue
            if (!(block.getState() instanceof Sign))
                continue;
            // Sign located!
            Sign tile = (Sign) block.getState();

            craft.removeUUIDMarkFromTile(tile);

            tile.update();
        }
        InteractListener.INTERACTION_TIME_MAP.remove(craft.getUUID());
    }
}
