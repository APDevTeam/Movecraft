package net.countercraft.movecraft.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RunnableRegistrationEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Map<Integer, List<Runnable>> values;

    public RunnableRegistrationEvent(Map<Integer, List<Runnable>> values) {
        this.values = values;
    }

    public void register(final int interval, final Runnable function) {
        int intervalActual = Math.max(interval, 1);
        this.values.computeIfAbsent(intervalActual, k -> new ArrayList<>()).add(function);
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
