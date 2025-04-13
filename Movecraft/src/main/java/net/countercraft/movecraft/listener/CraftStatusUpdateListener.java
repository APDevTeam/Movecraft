package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.features.status.events.CraftStatusUpdateEvent;
import net.countercraft.movecraft.util.ContactBlockHelper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CraftStatusUpdateListener implements Listener {

    @EventHandler
    public void onCraftStatusUpdate(final CraftStatusUpdateEvent event) {
        Craft craft = event.getCraft();
        if (craft instanceof PilotedCraft || craft instanceof SinkingCraft) {
            ContactBlockHelper.onCraftUpdate(craft);
        }
    }

}
