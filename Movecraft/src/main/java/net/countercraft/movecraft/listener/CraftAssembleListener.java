package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.SubCraft;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class CraftAssembleListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onAssembly(@NotNull CraftPilotEvent event) {
        // Walk through all signs and set a UUID in there
        final Craft craft = event.getCraft();

        // Now, find all signs on the craft...
        for (MovecraftLocation mLoc : craft.getHitBox()) {
            Block block = mLoc.toBukkit(craft.getWorld()).getBlock();
            // Only interested in signs, if no sign => continue
            // TODO: Just limit to signs?
            if (!(block.getState() instanceof TileState))
                continue;
            // Sign located!
            TileState tile = (TileState) block.getState();
            // Add the marker
            tile.getPersistentDataContainer().set(
                    MathUtils.KEY_CRAFT_UUID,
                    PersistentDataType.STRING,
                    craft.getUUID().toString()
            );
            tile.update();
        }
    }

    @EventHandler
    public void onDisassembly(@NotNull CraftReleaseEvent event) {
        // Walk through all signs and set a UUID in there
        final Craft craft = event.getCraft();

        // Now, find all signs on the craft...
        for (MovecraftLocation mLoc : craft.getHitBox()) {
            Block block = mLoc.toBukkit(craft.getWorld()).getBlock();
            // Only interested in signs, if no sign => continue
            if (!(block.getState() instanceof TileState))
                continue;
            // Sign located!
            TileState tile = (TileState) block.getState();

            if (craft instanceof SubCraft subcraft) {
                Craft parent = subcraft.getParent();
                if (parent != null) {
                    tile.getPersistentDataContainer().set(
                            MathUtils.KEY_CRAFT_UUID,
                            PersistentDataType.STRING,
                            parent.getUUID().toString()
                    );
                } else {
                    // Remove the marker
                    tile.getPersistentDataContainer().remove(MathUtils.KEY_CRAFT_UUID);
                }
            } else {
                // Remove the marker
                tile.getPersistentDataContainer().remove(MathUtils.KEY_CRAFT_UUID);
            }
            tile.update();
        }
    }
}
