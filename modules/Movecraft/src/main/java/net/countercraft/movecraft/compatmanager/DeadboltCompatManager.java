package net.countercraft.movecraft.compatmanager;

import net.countercraft.movecraft.events.SignTranslateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DeadboltCompatManager implements Listener {
    private static String PRIVATE = "[private]";
    @EventHandler
    public void onSignTranslate(SignTranslateEvent event){
        if (!event.getLine(0).equalsIgnoreCase(PRIVATE)){
            return;
        }

    }
}
