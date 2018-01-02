package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.api.events.SignTranslateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class SpeedSign implements Listener{
    @EventHandler
    public final void onSignTranslate(SignTranslateEvent event) {
        Craft craft = event.getCraft();
        if (!event.getLine(0).equalsIgnoreCase("Speed:")) {
            return;
        }
        event.setLine(1,String.format("%.2f",craft.getSpeed()) + "m/s");
    }
}
