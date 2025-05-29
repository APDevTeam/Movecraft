package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.util.ContactBlockHelper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CraftRotateListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onCraftRotate(final CraftRotateEvent event) {
        ContactBlockHelper.onRotate(event);
    }

}
