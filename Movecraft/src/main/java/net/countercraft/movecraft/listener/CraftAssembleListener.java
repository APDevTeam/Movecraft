package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
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
            if (!(block.getState() instanceof Sign))
                continue;
            // Sign located!
            Sign sign = (Sign) block.getState();
            // Add the marker
            sign.getPersistentDataContainer().set(
                    MathUtils.KEY_CRAFT_UUID,
                    PersistentDataType.STRING,
                    craft.getUUID().toString()
            );
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
            if (!(block.getState() instanceof Sign))
                continue;
            // Sign located!
            Sign sign = (Sign) block.getState();
            // Remove the marker
            sign.getPersistentDataContainer().remove(MathUtils.KEY_CRAFT_UUID);
        }
    }
}
