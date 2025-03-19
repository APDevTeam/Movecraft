package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.SinkingCraft;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CraftPostSinkEvent extends CraftEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final SinkingCraft sinkingCraft;

    public CraftPostSinkEvent(@NotNull Craft craft, SinkingCraft sinkingCraft) {
        super(craft);
        this.sinkingCraft = sinkingCraft;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public SinkingCraft getSinkingCraft() {
        return sinkingCraft;
    }
}
