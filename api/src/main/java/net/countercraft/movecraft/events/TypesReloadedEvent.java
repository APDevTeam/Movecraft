package net.countercraft.movecraft.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TypesReloadedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    
    public TypesReloadedEvent() {

    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
