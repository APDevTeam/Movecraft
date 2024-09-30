package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.TypesReloadedEvent;
import net.countercraft.movecraft.sign.AbstractMovecraftSign;
import net.countercraft.movecraft.sign.CraftPilotSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CraftTypeListener implements Listener {

    @EventHandler
    public void onReload(TypesReloadedEvent event) {
        AbstractMovecraftSign.registerCraftPilotSigns(CraftManager.getInstance().getCraftTypes(), CraftPilotSign::new);
    }

}
