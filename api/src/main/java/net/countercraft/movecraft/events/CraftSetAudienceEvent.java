package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import net.kyori.adventure.audience.Audience;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CraftSetAudienceEvent extends CraftEvent implements Cancellable {

    @NotNull private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled = false;

    public @Nullable Audience getOldAudience() {
        return oldAudience;
    }

    private @Nullable Audience oldAudience;

    public @Nullable Audience getNewAudience() {
        return newAudience;
    }

    public void setNewAudience(@Nullable Audience newAudience) {
        this.newAudience = newAudience;
    }

    private @Nullable Audience newAudience;

    public CraftSetAudienceEvent(@NotNull Craft craft, final Audience newAudience) {
        super(craft);
        this.oldAudience = craft.getAudience();
        this.newAudience = newAudience;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    // Cancelled is ignored if the current audience of the craft is null and the new one isnt null!
    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
