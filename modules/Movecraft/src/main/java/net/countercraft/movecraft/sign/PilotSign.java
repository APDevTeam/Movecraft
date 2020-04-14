package net.countercraft.movecraft.sign;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public final class PilotSign implements Listener {
    private static final String HEADER = "Pilot:";
    @EventHandler
    public final void onSignChange(SignChangeEvent event){
        if (event.getLine(0).equalsIgnoreCase(HEADER)) {
            String pilotName = ChatColor.stripColor(event.getLine(1));
            if (pilotName.isEmpty()) {
                event.setLine(1, event.getPlayer().getName());
            }
        }
    }
}
