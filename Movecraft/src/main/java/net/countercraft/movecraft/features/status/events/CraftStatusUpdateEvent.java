package net.countercraft.movecraft.features.status.events;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CraftStatusUpdateEvent extends CraftEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public CraftStatusUpdateEvent(@NotNull Craft craft) {
        super(craft, true);
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
