package net.countercraft.movecraft.sign;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
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

    public static synchronized boolean containsPilot(String name, Block signBlock){
        if (!(signBlock.getState() instanceof Sign)){
            throw new IllegalArgumentException("Block must be a sign!");
        }
        Sign sign = (Sign) signBlock.getState();
        boolean contains = false;
        if (sign.getLine(0).equalsIgnoreCase(HEADER)){
            for (int i = 1; i <= 3; i++){
                if (sign.getLine(i).equals(name)){
                    contains = true;
                    break;
                }
            }
        }
        return contains;
    }
}
