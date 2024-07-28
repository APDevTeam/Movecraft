package net.countercraft.movecraft.features.contacts.events;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class LostContactEvent extends CraftEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final @NotNull Craft target;

    public LostContactEvent(@NotNull Craft base, @NotNull Craft target) {
        super(base, true);
        this.target = target;
    }

    @NotNull
    public Craft getTargetCraft() {
        return target;
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
